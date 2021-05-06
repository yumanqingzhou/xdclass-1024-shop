package net.xdclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import net.xdclass.enums.BizCodeEnum;
import net.xdclass.enums.SendCodeEnum;
import net.xdclass.fegin.CouponFeginService;
import net.xdclass.interceptor.LoginInterceptor;
import net.xdclass.mapper.UserMapper;
import net.xdclass.model.LoginUser;
import net.xdclass.model.UserDO;
import net.xdclass.request.NewUserCouponRequest;
import net.xdclass.request.UserLoginRequest;
import net.xdclass.request.UserRegisterRequest;
import net.xdclass.service.NotifyService;
import net.xdclass.service.UserService;
import net.xdclass.utils.CommonUtil;
import net.xdclass.utils.JWTUtil;
import net.xdclass.utils.JsonData;
import net.xdclass.vo.UserVO;
import org.apache.commons.codec.digest.Md5Crypt;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private NotifyService notifyService;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private CouponFeginService couponFeginService;

    /**
     * 邮箱验证码验证
     * 密码加密（TODO）
     * 账号唯一性检查(TODO)
     * 插入数据库
     * 新注册用户福利发放(TODO)
     *
     * @param registerRequest
     * @return
     */
    @Override
    public JsonData register(UserRegisterRequest registerRequest) {
        //获取验证码
        String code = registerRequest.getCode();
        // 如果邮箱不为空 校验验证码
        Boolean checkCode = false;
        if (StringUtils.isNotBlank(registerRequest.getMail())) {
            //成功true
            checkCode = notifyService.checkCode(SendCodeEnum.USER_REGISTER, registerRequest.getMail(), registerRequest.getCode());
        }
        if (!checkCode) {
            //验证码错误直接返回
            return JsonData.buildResult(BizCodeEnum.CODE_ERROR);
        }
        //拷贝对象
        UserDO userDO = new UserDO();
        BeanUtils.copyProperties(registerRequest, userDO);
        userDO.setCreateTime(new Date());
        userDO.setSlogan("人生需要动态规划，学习需要贪心算法");
        //加盐设置设置密码
        String stringNumRandom = CommonUtil.getStringNumRandom(8);
        //盐值
        String crypt = "$1$" + stringNumRandom;
        //保存盐值
        userDO.setSecret(crypt);
        userDO.setPwd(Md5Crypt.md5Crypt(registerRequest.getPwd().getBytes(), crypt));
        //邮箱唯一性检查
        if (checkUnique(registerRequest.getMail())) {
            int rows = userMapper.insert(userDO);
            log.info("rows:{},注册成功:{}", rows, userDO.toString());
            //新用户注册成功，初始化信息，发放福利等 TODO
            userRegisterInitTask(userDO);
            return JsonData.buildSuccess();
        } else {
            return JsonData.buildError(BizCodeEnum.ACCOUNT_REPEAT.getMessage());
        }

    }

    /**
     * 用户登录
     * 检查账号是否存在
     * 判断密码是否相等
     * @param userLoginRequest
     * @return
     */
    @Override
    public JsonData login(UserLoginRequest userLoginRequest) {
        String mail = userLoginRequest.getMail();
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        wrapper.eq("mail",userLoginRequest.getMail());
        List<UserDO> userDOS = userMapper.selectList(wrapper);
        if (userDOS!=null&&userDOS.size()==1){
            //存在 判断密码是否相等
            UserDO userDO = userDOS.get(0);
            String secret = userDO.getSecret();
            String MD5pwd = Md5Crypt.md5Crypt(userLoginRequest.getPwd().getBytes(), secret);
            if (MD5pwd.equalsIgnoreCase(userDO.getPwd())){
                //登录成功 生成token
                LoginUser loginUser=new LoginUser();
                BeanUtils.copyProperties(userDO, loginUser);
                String token = JWTUtil.geneJsonWebToken(loginUser);
                return JsonData.buildSuccess(token);
            }else {
                JsonData.buildResult(BizCodeEnum.ACCOUNT_PWD_ERROR);
            }
        }else {
            return JsonData.buildResult(BizCodeEnum.ACCOUNT_UNREGISTER);
        }
        return null;
    }

    /**
     * 查询用户信息详情
     * @return
     */
    @Override
    public UserVO findUserDetail() {
        LoginUser loginUser = LoginInterceptor.threadLocal.get();
        UserDO userDO = userMapper.selectOne(new QueryWrapper<UserDO>().eq("id", loginUser.getId()));
        UserVO userVO=new UserVO();;
        BeanUtils.copyProperties(userDO,userVO);
        return userVO;
    }

    /**
     * 校验用户账号唯一
     *
     * @param mail
     * @return
     */
    private boolean checkUnique(String mail) {
        QueryWrapper<UserDO> wrapper = new QueryWrapper<>();
        wrapper.eq("mail", mail);
        List<UserDO> userDOS = userMapper.selectList(wrapper);
        return userDOS.size() > 0 ? false : true;
    }

    /**
     * 用户注册，初始化福利信息 TODO
     *
     * @param userDO
     */
    private void userRegisterInitTask(UserDO userDO) {
        NewUserCouponRequest request = new NewUserCouponRequest();
        request.setName(userDO.getName());
        request.setUserId(userDO.getId());
        JsonData jsonData = couponFeginService.addNewUserCoupon(request);
        log.info("发放新用户注册优惠券：{},结果:{}",request.toString(),jsonData.toString());
    }

}
