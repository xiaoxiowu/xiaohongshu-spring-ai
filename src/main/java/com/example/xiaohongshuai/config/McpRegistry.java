package com.example.xiaohongshuai.config;

public final class McpRegistry {

    private McpRegistry() {
    }

    public static final class Connections {

        public static final String XIAOHONGSHU = "xiaohongshu";
        public static final String JIMENG = "jimeng";

        private Connections() {
        }
    }

    public static final class Tools {

        public static final String XHS_CHECK_LOGIN_STATUS = "check_login_status";
        public static final String XHS_GET_LOGIN_QRCODE = "get_login_qrcode";
        public static final String XHS_DELETE_COOKIES = "delete_cookies";
        public static final String XHS_SEARCH_FEEDS = "search_feeds";
        public static final String XHS_GET_FEED_DETAIL = "get_feed_detail";
        public static final String XHS_PUBLISH_CONTENT = "publish_content";
        public static final String XHS_PUBLISH_WITH_VIDEO = "publish_with_video";
        public static final String JIMENG_GENERATE_IMAGE = "jimeng-v40-generate";

        private Tools() {
        }
    }

    public static final class Env {

        public static final String JIMENG_ACCESS_KEY = "JIMENG_ACCESS_KEY";
        public static final String JIMENG_SECRET_KEY = "JIMENG_SECRET_KEY";

        private Env() {
        }
    }

    public static final class AssetSources {

        public static final String JIMENG = "JIMENG";
        public static final String URL = "URL";

        private AssetSources() {
        }
    }
}
