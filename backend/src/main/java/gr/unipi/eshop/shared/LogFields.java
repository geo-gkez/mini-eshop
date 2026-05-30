package gr.unipi.eshop.shared;

import lombok.experimental.UtilityClass;

@UtilityClass
public class LogFields {

    @UtilityClass
    public class Key {
        public final String EVENT = "event";
        public final String USER = "user";
        public final String PATH = "path";
        public final String REF = "ref";
        public final String PRODUCT = "product";
        public final String QUANTITY = "quantity";
        public final String SEARCH = "search";
        public final String PAGE = "page";
        public final String SIZE = "size";
    }

    @UtilityClass
    public class Event {
        public final String LOGIN_SUCCESS = "LOGIN_SUCCESS";
        public final String LOGIN_FAILURE = "LOGIN_FAILURE";
        public final String LOGOUT = "LOGOUT";
        public final String UNAUTHENTICATED_ACCESS = "UNAUTHENTICATED_ACCESS";
        public final String ACCESS_DENIED = "ACCESS_DENIED";
        public final String CART_ITEM_ADDED = "CART_ITEM_ADDED";
        public final String CART_ITEM_UPDATED = "CART_ITEM_UPDATED";
        public final String CART_ITEM_REMOVED = "CART_ITEM_REMOVED";
        public final String PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
        public final String CART_FULL = "CART_FULL";
        public final String VALIDATION_FAILURE = "VALIDATION_FAILURE";
        public final String CATALOG_LIST    = "CATALOG_LIST";
        public final String ORDER_SUBMITTED = "ORDER_SUBMITTED";
        public final String ORDER_CONFIRMED = "ORDER_CONFIRMED";
    }
}
