package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class UserService {

   @Autowired
   private UserMapper userMapper;

   @Autowired
   private AmqpTemplate amqpTemplate;

   @Autowired
   private StringRedisTemplate redisTemplate;

   private static final String KEY_PREFIX = "user:verigy:";

   private static final Logger logger = LoggerFactory.getLogger(UserService.class);

   public Boolean checkData(String data, Integer type) {
      User record = new User();
      switch (type) {
         case 1:
            record.setUsername(data);
            break;
         case 2:
            record.setPhone(data);
            break;
         default:
            return null;
      }
      return this.userMapper.selectCount(record) == 0;
   }

   public Boolean sendVerifyCode(String phone) {
      if (StringUtils.isBlank(phone)){
         return false;
      }

      // 生成验证码
      String code = NumberUtils.generateCode(6);

      try {
         // 发送消息发哦rabbitMQ
         HashMap<String, String> msg = new HashMap<>();
         msg.put("phone", phone);
         msg.put("code", code);
         this.amqpTemplate.convertAndSend("leyou.sms.exchange","verifycode.sms", msg);

         // 把验证码code存入redis
         this.redisTemplate.opsForValue().set(KEY_PREFIX+phone, code, 5, TimeUnit.MINUTES);
         return true;
      } catch (Exception e) {
         logger.error("发送短信失败。phone：{}， code：{}", phone, code);
         return false;
      }
   }

   public Boolean register(User user, String code) {
      // 查询redis中验证码
      String redisCacheCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
      // 校验短信验证码
      if (!StringUtils.equals(code, redisCacheCode)) {
         return false;
      }

      // 生成盐
      String salt = CodecUtils.generateSalt();
      user.setSalt(salt);

      // 对密码加密
      user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

      // 强制设置不能指定的参数为null
      user.setId(null);
      user.setCreated(new Date());
      // 添加到数据库
      boolean b = this.userMapper.insertSelective(user) == 1;

      if(b){
         // 注册成功，删除redis中的记录
         this.redisTemplate.delete(KEY_PREFIX + user.getPhone());
      }
      return b;
   }

   public User queryUser(String username, String password) {
      // 查询
      User record = new User();
      record.setUsername(username);
      User user = this.userMapper.selectOne(record);
      // 校验用户名
      if (user == null) {
         return null;
      }

      // 获取盐，对用户输入的密码加盐加密
      password = CodecUtils.md5Hex(password, user.getSalt());
      // 校验密码，和数据库中的密码比较
      if (StringUtils.equals(password, user.getPassword())) {
         return user;
      }
      return null;
   }
}