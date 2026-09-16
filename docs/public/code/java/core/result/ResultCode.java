package com.sjbb.core.result;

/**
 * 响应码枚举，参考HTTP状态码的语义
 */
public enum ResultCode {
    /****************Global******************/
    SUCCESS(200, "SUCCESS"),//成功
    FAIL(400, "ERROR"),//失败
    UNAUTHORIZED(401, "没有权限,请先登录"),//未认证（签名错误）
    NOT_FOUND(404, "APi not find"),//接口不存在
    INTERNAL_SERVER_ERROR(500, "Server Error"),//服务器内部错误
    SERVICE_UNAVAILABLE(503, "服务器打了个盹，请稍后重试"),
    GATEWAY_TIMEOUT(504, "请求超时"),
    THIRDPATY_SERVICE_ERROR(505, "竞猜功能维护中"),
    DO_NOT_HAVE_SYSTEM_SERVICE(506, "系统维护中"),
    /*****系统业务 system*****/

    SYSTEM_GET_CONFIG_FAILD(1001, "获取系统资源配置失败/未知错误"),
    SYSTEM_UPDATE_CONFIG_FAILD(1004, "修改系统资源配置失败/未知错误"),
    SYSTEM_UPDATE_CONFIG_INVALID_PARAM(1005, "无效的系统资源配置名称"),
    SYSTEM_UPDATE_CONFIG_INVALID_VALUE(1006, "系统状态输入格式错误"),

    RED_PACKET_CONFIG_ADD_FAILD(1013, "添加红包配置信息失败/未知错误"),
    RED_PACKET_CONFIG_ADD_PACKET_VALUE_ERROR(1014, "添加随机红包金额范围必须为整数且大于0"),
    RED_PACKET_CONFIG_ADD_RANGE_VALUE_ERROR(1015, "添加红包金额格式设置错误"),
    RED_PACKET_CONFIG_ADD_END_TIME_PRE_ERROR(1016, "添加红包结束时间不得在开始领取时间之前"),
    RED_PACKET_CONFIG_ADD_TIME_FORMAT_ERROR(1017, "添加红包时间格式设置错误"),
    RED_PACKET_CONFIG_ADD_TOTAL_PACKET_AMOUNT_ERROR(1018, "添加红包总金额为整数且大于0"),
    RED_PACKET_CONFIG_ADD_TOTAL_PACKENT_NUM_ERROR(1019, "添加红包总数量为整数且大于0"),
    RED_PACKET_CONFIG_ADD_TYPE_ERROR(1020, "添加红包类型type错误(1/2)"),
    RED_PACKET_CONFIG_ADD_TIME_CASH(1021, "添加红包可抢时间段与现有记录时间段冲突"),


    RED_PACKET_CONFIG_UPT_FAILD(1028, "修改红包配置信息失败/未知错误"),
    RED_PACKET_CONFIG_UPT_PACKET_VALUE_ERROR(1029, "修改随机红包金额范围必须为整数且大于0"),
    RED_PACKET_CONFIG_UPT_RANGE_VALUE_ERROR(1030, "修改随机红包金额格式设置错误"),
    RED_PACKET_CONFIG_UPT_END_TIME_PRE_ERROR(1031, "修改红包结束时间不得在开始领取时间之前"),
    RED_PACKET_CONFIG_UPT_TIME_FORmAT_ERROR(1032, "修改红包时间格式设置错误"),
    RED_PACKET_CONFIG_UPT_TOTAL_PACKET_AMOUNT_ERROR(1033, "修改红包总金额为整数且大于0"),
    RED_PACKET_CONFIG_UPT_TOTAL_PACKET_NUM_ERROR(1034, "修改红包总数量为整数且大于0"),
    RED_PACKET_CONFIG_UPT_TYPE_ERROR(1022, "修改红包类型type错误(1/2)"),
    RED_PACKET_CONFIG_UPT_ID_NOT_EXSIT(1023, "修改红包配置ID输入错误/暂无该ID的红包配置"),
    RED_PACKET_CONFIG_UPT_TIME_CASH(1024, "修改红包可抢时间段与现有记录时间段冲突"),
    RED_PACKET_CONFIG_UPT_LOCKET(1025, "该红包配置已锁定，无法修改"),


    RED_PACKET_CONFIG_DEL_FAIL(1043, "删除红包配置信息失败/未知错误"),
    RED_PACKET_CONFIG_DEL_ID_NOT_EXSIT(1044, "删除红包配置信息失败/未知错误"),
    RED_PACKET_CONFIG_DEL_LOCKET(1045, "该红包配置已锁定，无法删除"),

    SING_CONFIG_FAIL(1046, "获取签到配置信息失败/未知错误"),
    EXCHANGE_INVITATION_CODE_REPEAT(1047, "您已兑换过邀请码"),
    EXCHANGE_INVITATION_CODE_ERROR(1048, "请输入正确的邀请码"),
    EXCHANGE_INVITATION_CODE_NOT_MYSELF(1049, "不能使用自己的邀请码"),
    EXCHANGE_INVITATION_CODE_EACH_OTHER(1050, "不能兑换徒弟邀请码"),


    RECHARGE_CONFIG_LEVEL_ERROR(1055, "充值配置等级错误"),
    RECHARGE_CONFIG_NAME_EMPTY(1056, "充值配置名称不能为空"),
    RECHARGE_CONFIG_IS_USED(1057, "充值配置正在使用"),

    ADD_VERSION_NUM_ERROR(1060, "版本号不能低于当前最新版本号"),
    ADD_CROSS_VERSION_ERROR(1061, "还有版本未发布，不能新建新版本"),
    UPDATE_VERSION_NUM_ERROR(1062, "版本号不能低于当前最新版本号"),
    UPDATE_VERSION_ERROR(1063, "版本已发布，不能修改"),
    UPDATE_CROSS_VERSION_ERROR(1064, "当前还有版本未发布，不能转换客户端或版本类型"),
    DEL_VERSION_NUM_ERROR(1065, "版本已发布，不能删除"),
    VERSION_CLIENT_TYPE_ERROR(1066, "客戶端类型格式错误"),
    VERSION_TYPE_ERROR(1067, "客戶端类型格式错误"),
    VERSION_VERSION_ERROR(1068, "版本号格式错误"),
    VERSION_COMPATIBLE_ERROR(1069, "版本兼容性格式错误"),
    VERSION_PUBLISH_ERROR(1070, "未检测到新版本"),


    USER_LOGIN_FAILD(3000, "登录失败/未知错误"),
    USER_LOGIN_ACCOUNT_NOT_EXIST(3001, "账号不存在"),
    USER_LOGIN_LOGIN_PASSWORD_ERROR(3002, "密码错误"),
    USER_LOGIN_PASSWORD_ERROR_TOMUCH(3003, "密码多次输入错误，账号已暂时锁定，30分钟后再试"),
    USER_LOGIN_FORBIDDEN(3004, "用户被禁止登录"),

    USER_MESSAGE_ALIYUN_SEND_CODE_FAILD(3006, "验证码发送失败"),
    USER_MESSAGE_PHONE_ERROR(3007, "手机号码错误"),
    USER_MESSAGE_TYPE_ERROR(3008, "短信类型TYPE错误"),

    USER_TOKEN_REFRESH_FAILD(3009, "TOKEN不存在（或已失效）"),

    USER_REGISTER_FAILD(3010, "注册失败/未知错误"),
    USER_REGISTER_PHONE_ERROR(3011, "手机号码错误"),
    USER_REGISTER_ACCOUNT_EXIST(3012, "手机号码已注册"),
    USER_REGISTER_PASSWORD_FORMAT_ERROR(3013, "密码格式错误"),
    USER_REGISTER_CODE_EXPIRED(3013, "验证码错误/过期失效"),

    USER_RESETPWD_FAILD(3018, "密码重置失败/未知错误"),
    USER_RESETPWD_PHONE_ERROR(3019, "手机号码错误"),
    USER_RESETPWD_NEWPWD_SAME_ERROR(3020, "新密码不能与旧密码相同"),
    USER_RESETPWD_NEWPWD_FORMAT_ERROR(3021, "新密码格式错误"),
    USER_RESETPWD_CODE_EXPERID(3022, "验证码错误/过期失效"),
    USER_RESETPWD_PHONE_NOT_EXIST(3023, "该手机号码暂未注册"),


    USER_BINDINFO_FAILD(3027, "注册信息绑定失败/未知错误"),
    USER_BINDINFO_ID_ERROR(3028, "绑定id错误"),
    USER_BINDINFO_NAME_FORMAT_ERROR(3029, "名称格式错误/长度过长"),
    USER_BINDINFO_SEX_ERROR(3030, "性别格式错误"),

    USER_LIST_FAILD(3100, "获取用户列表失败/未知错误"),
    USER_LIST_TIME_FORMAT_ERROR(3101, "开始或结束时间格式错误"),
    USER_LIST_STATUS_FROMAT_ERROR(3102, "账号状态码status错误(0/1)"),

    USER_UPDATE_FAILD(3200, "修改基本信息失败/未知错误"),
    USER_UPDATE_AVATAR_FROMAT_ERROR(3201, "头像类型错误(0~5)"),
    USER_UPDATE_NICKNAME_ERROR(3202, "昵称格式错误/长度过长"),
    USER_UPDATE_SEX_FORMAT_ERROR(3203, "性别格式错误(1/2)"),
    USER_UPDATE_NICKNAME_TIME_NOT_FIX(3204, "两次修改昵称时间间隔需大于90天"),
    USER_UPDATE_PUSH_CODE_ERROR(3205, "消息推送编码错误"),

    USER_GETINFO_FAILD(3207, "获取个人用户信息失败/未知错误"),
    USER_GETINFO_TOKEN_EXPERID(3208, "登录已过期/Token过期"),

    SIGN_REPEAT_ERROR(3306, "您今天已经签到过了"),
    REDPACKET_OPEN_EMPTY(3307, "红包发完了"),
    REDPACKET_NOT_OPEN_TIME(3308, "当前不在抢红包时间段"),
    REDPACKET_OPEN_ALREADY(3309, "您已经领取过该红包了"),
    REDPACKET_OPEN_NOT_PROMISE(3310, "抱歉，您暂无抢红包资格(完成任意模式投注可获得抢红包资格)"),

    /*****后台权限管理 admin*****/
    ADMIN_USER_LOGIN_USERNAME_OR_PASSWORD_ERROR(2001, "用户名/密码错误"),
    ADMIN_USER_USERNAME_EXIST(2101, "用户名已存在"),
    ADMIN_DEPTMENT_DELETE_USER_EXIST(2201, "部门下存在用户，无法删除!"),
    ADMIN_USER_EDITPWD_PWDERROR(2301, "密码输入错误!"),

    /**
     * 资讯
     */
    ADMIN_INFORMATION_TYPE_DELETE_NOT_EMPTY(2400, "该类型下尚有资讯，不能删除"),

    /**
     * 商品
     */
    ADMIN_PRODUCT_DELETE_ORDER_NOT_EMPTY(2500, "有订单关联该商品，不能删除"),
    ADMIN_PRODUCT_HOT_SET_MAXNUM(2501, "最多只能设置6个热门"),
    ADMIN_PRODUCT_TIME_LIMIT_MAXNUM(2502, "最多只能上架5个限时商品"),

    /*****App用户管理 user*****/
    BUY_GOLD_DIAMOND_NOT_ENOUGH(6001, "钻石余额不足"),

    /*****竞猜业务 match*****/
    SINGLE_ORDER_ADD_BETREGIONTYPE_ERROR(5001, "投注盘口类型错误"),
    SINGLE_ORDER_ADD_BETTYPE_ERROR(5002, "投注订单类型错误"),
    SINGLE_ORDER_ADD_BETTIMETYPE_ERROR(5003, "投注订单时间类型错误"),
    SINGLE_ORDER_ADD_BETOPTION_ERROR(5004, "投注选项参数类型错误"),
    SINGLE_ORDER_ADD_BETOPTIONKEY_ERROR(5005, "投注选项参数KEY类型错误"),
    SINGLE_ORDER_ADD_BETOPTIONNAME_ERROR(5006, "投注选项名称错误"),
    SINGLE_ORDER_ADD_BETGOLD_ERROR(5007, "投注金额类型错误"),
    SINGLE_ORDER_ADD_SPORTID_ERROR(5008, "投注赛事id格式错误"),
//    SINGLE_ORDER_ADD_BETOPTIONVAL_ERROR(5009, "投注赛事赔率错误"),

    QUIZ_BETSTATUS_ERROR(5022, "玩法总数类型错误"),
    QUIZ_ENABLE_ERROR(5023, "竞猜比赛启用停用类型错误"),
    QUIZ_STATE_ERROR(5024, "开奖状态类型错误"),

    SET_QUIZ_ID_IS_EMPTY(5031, "id不能为空"),
    SET_QUIZ_ENABLE_ERROR(5032, "竞猜比赛启用停用类型错误"),

    GET_BET_ODDS_IS_NULL(5555, "该下注项已封盘"),

    SPORT_BET_INFO_SPORT_DISABLE(5556, "该比赛已封盘"),

    ORDER_ADD_SPORT_STATE_ERROR(5558, "投注赛事状态不符合下单类型"),
    ORDER_ADD_SPORT_DISABLE(5559, "投注赛事已封盘"),
    ORDER_ADD_SPORT_BET_ENDED(5560, "投注赛事玩法已封盘"),
    ORDER_ADD_BET_OPTIONS_IS_NULL(5561, "赛事玩法未选择"),

    FIRST_RECHARGE_GIFT_GET_ERROR(6000, "用户没有首冲奖励"),
    NEWCOMER_REWARD_RECIVE_ERROR(6001, "用户没有新人奖励"),

    SPORT_SUGG_SPORT_NOT_EXSIT(9000, "赛事或玩法不存在"),
    SPORT_SUGG_SPORT_INVAILD(9001, "赛事失效"),
    SPORT_SUGG_SPORT_ERROR(9002, "赛事不完整"),
    SPORT_SUGG_SPORT_TIME_ERROR(9003, "赛事时间不匹配"),
    SPORT_SUGG_SPORT_STATE_ERROR(9004, "赛事状态不对"),

    /*****商城业务 shop*****/
    USER_ADDRESS_ID_ERROR(9101, "用户地址不匹配"),

    /*****社交业务 social*****/


    END(9999, "结束用");


    public final int code;
    public final String message;


    public int getCode() {
        return code;
    }


    public String getMessage() {
        return message;
    }

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
