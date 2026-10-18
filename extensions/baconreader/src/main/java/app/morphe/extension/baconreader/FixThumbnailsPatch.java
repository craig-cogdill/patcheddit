/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.baconreader;

import java.lang.reflect.Field;

public final class FixThumbnailsPatch {
    private static volatile Field thumbnailField;

    private FixThumbnailsPatch() {
    }

    /**
     * Reddit HTML-escapes '&amp;' in the JSON thumbnail field. Thumbnails now live on
     * preview.redd.it behind a signed query string, so the escaped URL fails with 403.
     * Unescape the URL in place on the Link object (public field "thumbnail").
     */
    public static void fixLink(Object link) {
        if (link == null) {
            return;
        }
        try {
            Field field = thumbnailField;
            if (field == null) {
                field = link.getClass().getField("thumbnail");
                thumbnailField = field;
            }
            Object value = field.get(link);
            android.util.Log.i("FixThumbnails", "thumbnail=" + value);
            if (value instanceof String) {
                String url = (String) value;
                if (url.indexOf("&amp;") >= 0) {
                    field.set(link, url.replace("&amp;", "&"));
                }
            }
        } catch (Exception ignored) {
            // Leave the thumbnail untouched rather than crash the feed.
        }
    }
}
