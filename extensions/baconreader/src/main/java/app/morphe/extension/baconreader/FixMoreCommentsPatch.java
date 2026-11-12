/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.baconreader;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Since late October 2026 Reddit answers every /api/morechildren request from the
 * RedReader client ID with "403 Blocked", so "load more comments" fails. BaconReader
 * sends that request through its own Apache-based RestClient, which the shared
 * OkHttp-based fix cannot see.
 *
 * This hook runs at the start of RedditApi.httpRequest(). For api/morechildren it loads
 * each hidden comment from the normal comment permalink instead (one GET per comment,
 * with its parent chain for the correct depth, which also returns that comment's replies), then builds a response in the
 * morechildren shape the app already parses. Every other request passes through.
 */
public final class FixMoreCommentsPatch {
    private static final String TAG = "MorpheMoreComments";
    private static final String MORE_CHILDREN_PATH = "api/morechildren";

    /**
     * Reddit's OAuth limit is about 100 requests per minute. Load at most this many
     * hidden comments per tap. The rest go back as a new "load more comments" row.
     */
    private static final int MAX_REQUESTS_PER_TAP = 20;

    private static volatile Method httpRequestMethod;

    private FixMoreCommentsPatch() {
    }

    /**
     * @return the replacement response body, or null to let the app send its own request.
     */
    public static String loadMoreChildren(Object context, Object requestMethod, String path,
                                          Map<String, String> params, Object sessionToken)
            throws IOException {
        if (!MORE_CHILDREN_PATH.equals(path) || params == null) {
            return null;
        }

        try {
            String postId = stripPrefix(params.get("link_id"));
            String children = params.get("children");
            if (postId == null || postId.isEmpty() || children == null || children.isEmpty()) {
                return null;
            }

            LinkedHashSet<String> targetIds = new LinkedHashSet<>();
            for (String child : children.split(",")) {
                String id = stripPrefix(child.trim());
                if (id != null && !id.isEmpty()) targetIds.add(id);
            }
            if (targetIds.isEmpty()) return null;
            Log.i(TAG, "start post=" + postId + " children=" + targetIds.size()
                    + " sort=" + params.get("sort") + " token=" + (sessionToken != null));
            long started = System.currentTimeMillis();

            Object getMethod = enumConstant(requestMethod, "GET");
            Method httpRequest = httpRequest(context, requestMethod, sessionToken);

            JSONArray things = new JSONArray();
            Set<String> emittedIds = new HashSet<>();
            Set<String> emittedNames = new HashSet<>();
            List<String> leftover = new ArrayList<>();
            String firstParentId = null;
            int requests = 0;

            for (String childId : targetIds) {
                if (emittedIds.contains(childId)) continue;
                if (requests >= MAX_REQUESTS_PER_TAP) {
                    leftover.add(childId);
                    continue;
                }
                requests++;

                Map<String, String> query = new HashMap<>();
                // Ask for the parent chain too. Reddit then numbers "depth" from the top-level
                // comment, and the app indents each comment by that number.
                query.put("context", "8");
                String sort = params.get("sort");
                if (sort != null && !sort.isEmpty()) query.put("sort", sort);

                long t0 = System.currentTimeMillis();
                String body = invoke(httpRequest, context, getMethod,
                        "comments/" + postId + "/-/" + childId + "/.json", query, sessionToken);
                Log.i(TAG, "child " + childId + " " + (System.currentTimeMillis() - t0) + "ms");

                JSONArray listing = new JSONArray(body);
                JSONArray focused = listing.getJSONObject(1).getJSONObject("data").getJSONArray("children");
                if (focused.length() == 0) continue; // Deleted or removed.

                JSONObject thing = findComment(focused.getJSONObject(0), childId);
                if (thing == null) continue; // Deleted or removed.
                JSONObject data = thing.getJSONObject("data");
                if (firstParentId == null) firstParentId = data.optString("parent_id", null);

                flattenThing(thing, emittedIds, emittedNames, things);
            }

            // Comments not loaded this time become a new "load more comments" row. They
            // share the parent of the hidden group, which is the parent of the first one.
            leftover.removeAll(emittedIds);
            if (!leftover.isEmpty() && firstParentId != null) {
                JSONObject more = new JSONObject()
                        .put("id", leftover.get(0))
                        .put("name", "t1_" + leftover.get(0))
                        .put("parent_id", firstParentId)
                        .put("count", leftover.size())
                        .put("children", new JSONArray(leftover));
                things.put(new JSONObject().put("kind", "more").put("data", more));
            }

            JSONObject json = new JSONObject()
                    .put("errors", new JSONArray())
                    .put("data", new JSONObject().put("things", things));
            String result = new JSONObject().put("json", json).toString();
            Log.i(TAG, "done requests=" + requests + " things=" + things.length() + " leftover="
                    + leftover.size() + " " + (System.currentTimeMillis() - started) + "ms");
            return result;
        } catch (IOException e) {
            Log.w(TAG, "Loading hidden comments failed", e);
            throw e;
        } catch (Throwable e) {
            // Unexpected shape or reflection failure. Fall back to the app's own request.
            Log.e(TAG, "Could not load hidden comments", e);
            return null;
        }
    }

    /**
     * Writes the comment, then each reply in thread order. The app works out nesting from
     * that order and each item's parent_id, the same as a real morechildren response.
     */
    private static void flattenThing(JSONObject thing, Set<String> emittedIds,
                                     Set<String> emittedNames, JSONArray output)
            throws JSONException {
        String kind = thing.optString("kind");
        JSONObject data = thing.getJSONObject("data");

        if ("t1".equals(kind)) {
            Object replies = data.opt("replies");
            if (emittedIds.add(data.getString("id"))) {
                data.put("replies", "");
                output.put(thing);
                emittedNames.add(data.getString("name"));
            }
            if (replies instanceof JSONObject) {
                JSONObject repliesData = ((JSONObject) replies).optJSONObject("data");
                JSONArray children = repliesData == null ? null : repliesData.optJSONArray("children");
                if (children != null) {
                    for (int i = 0; i < children.length(); i++) {
                        flattenThing(children.getJSONObject(i), emittedIds, emittedNames, output);
                    }
                }
            }
        } else if ("more".equals(kind) && emittedNames.contains(data.optString("parent_id"))) {
            output.put(thing);
        }
    }

    /**
     * The response starts at the oldest parent. Walk down its replies to the comment asked for.
     */
    private static JSONObject findComment(JSONObject thing, String id) throws JSONException {
        if (!"t1".equals(thing.optString("kind"))) return null;
        JSONObject data = thing.getJSONObject("data");
        if (id.equals(data.optString("id"))) return thing;
        Object replies = data.opt("replies");
        if (!(replies instanceof JSONObject)) return null;
        JSONObject repliesData = ((JSONObject) replies).optJSONObject("data");
        JSONArray children = repliesData == null ? null : repliesData.optJSONArray("children");
        if (children == null) return null;
        for (int i = 0; i < children.length(); i++) {
            JSONObject found = findComment(children.getJSONObject(i), id);
            if (found != null) return found;
        }
        return null;
    }

    private static Method httpRequest(Object context, Object requestMethod, Object sessionToken)
            throws ReflectiveOperationException {
        Method method = httpRequestMethod;
        if (method != null) return method;

        Class<?> api = Class.forName("com.onelouder.baconreader.reddit.RedditApi");
        for (Method candidate : api.getDeclaredMethods()) {
            Class<?>[] types = candidate.getParameterTypes();
            if (candidate.getName().equals("httpRequest") && types.length == 5
                    && types[1].isInstance(requestMethod)
                    && types[2] == String.class
                    && Map.class.isAssignableFrom(types[3])) {
                candidate.setAccessible(true);
                httpRequestMethod = candidate;
                return candidate;
            }
        }
        throw new NoSuchMethodException("RedditApi.httpRequest");
    }

    private static String invoke(Method httpRequest, Object... args)
            throws IOException, IllegalAccessException {
        try {
            return (String) httpRequest.invoke(null, args);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof IOException) throw (IOException) cause;
            if (cause instanceof RuntimeException) throw (RuntimeException) cause;
            throw new IOException(cause);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumConstant(Object sample, String name) {
        return Enum.valueOf(((Enum) sample).getDeclaringClass(), name);
    }

    private static String stripPrefix(String fullName) {
        if (fullName == null) return null;
        return fullName.length() > 3 && fullName.charAt(2) == '_' ? fullName.substring(3) : fullName;
    }
}
