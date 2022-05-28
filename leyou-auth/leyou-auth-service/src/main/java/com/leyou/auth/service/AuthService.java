package com.leyou.auth.service;

import com.leyou.auth.client.UserClient;
import com.leyou.auth.config.JwtProperties;
import com.leyou.auth.pojo.UserInfo;
import com.leyou.auth.utils.JwtUtils;
import com.leyou.user.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired
    private UserClient userClient;

    @Autowired
    private JwtProperties jwtProperties;

    public String accredit(String username, String password) {

        // 1.根据用户名和密码查询
        User user = this.userClient.queryUser(username, password);

        // 2. 判断user是否为空
        if (user == null){
            return null;
        }

        // 3. 使用jwtUtils生成jwt类型的token
        UserInfo userInfo = new UserInfo();
        userInfo.setUsername(user.getUsername());
        userInfo.setId(user.getId());
        try {
            return JwtUtils.generateToken(userInfo, jwtProperties.getPrivateKey(), jwtProperties.getExpire()*60);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
