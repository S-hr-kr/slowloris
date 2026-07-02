package com.slowloris.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.slowloris.auth.entity.WxUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface WxUserMapper extends BaseMapper<WxUser> {

    @Select("SELECT * FROM wx_user WHERE openid = #{openid}")
    WxUser findByOpenid(@Param("openid") String openid);
}
