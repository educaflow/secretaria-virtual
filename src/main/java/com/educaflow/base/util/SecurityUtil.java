package com.educaflow.base.util;

import com.axelor.auth.AuthUtils;
import com.axelor.auth.db.User;

public class SecurityUtil {

    public static User getUser() {
        return AuthUtils.getUser();
    }


}
