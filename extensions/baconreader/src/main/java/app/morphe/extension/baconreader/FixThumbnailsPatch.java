/*
 * Copyright 2026 wchill.
 * https://github.com/wchill/patcheddit
 *
 * See the included NOTICE file for GPLv3 §7(b) and §7(c) terms that apply to this code.
 */

package app.morphe.extension.baconreader;

public final class FixThumbnailsPatch {
    private FixThumbnailsPatch() {
    }

    /**
     * Reddit HTML-escapes '&amp;' in the JSON thumbnail field. Thumbnails now live on
     * preview.redd.it behind a signed query string, so the escaped URL fails with 403.
     */
    public static String fixUrl(String url) {
        if (url == null) {
            return null;
        }
        return url.replace("&amp;", "&");
    }
}
