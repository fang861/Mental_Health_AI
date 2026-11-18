package org.example.aispringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.aispringboot.entity.ConsultationMessage;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper
public interface ConsultationMessageMapper extends BaseMapper<ConsultationMessage> {

}
