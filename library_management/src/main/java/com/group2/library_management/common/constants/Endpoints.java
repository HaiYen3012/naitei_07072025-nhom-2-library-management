package com.group2.library_management.common.constants;

/**
 * Lớp final chứa các hằng số cho các endpoints của API.
 * Sử dụng các lớp tĩnh để nhóm các endpoint theo tài nguyên.
 */
public final class Endpoints {

    private Endpoints() {
    }

    private static final String API_BASE = "/api";
    private static final String ADMIN_BASE = "/admin";

    public static final class ApiV1 {
        private static final String BASE = API_BASE + "/v1";

        public static final class Auth {
            private Auth() {}

            public static final String BASE_URL = API_BASE + "/auth";
            public static final String REGISTER_ACTION = "/register";
            public static final String LOGIN_ACTION = "/login";
            public static final String REFRESH_ACTION = "/refresh";
            public static final String LOGOUT_ACTION = "/logout";

            public static final String REGISTER_ENDPOINT = BASE_URL + REGISTER_ACTION;
            public static final String LOGIN_ENDPOINT = BASE_URL + LOGIN_ACTION;
            public static final String REFRESH_ENDPOINT = BASE_URL + REFRESH_ACTION;
            public static final String LOGOUT_ENDPOINT = BASE_URL + LOGOUT_ACTION;
        }

        public static final class Books {
            private Books() {}

            public static final String BASE_URL = BASE + "/books";
            public static final String BY_ID_ACTION = "/{id}";

            public static final String ALL_BOOKS_PATHS = BASE_URL + "/**"; 
        }

        public static final class Cart {
            private Cart() {}
            
            public static final String BASE_URL = BASE + "/cart";
            public static final String ADD_ITEM_ACTION = "/add";
        }

        public static final class Borrowings {
            private Borrowings() {}

            public static final String BASE_URL = BASE + "/borrowings";
            public static final String CREATE_REQUEST_ACTION = "/request";
        }
    }

    public static final class Admin {
        private Admin() {}
    }
}
