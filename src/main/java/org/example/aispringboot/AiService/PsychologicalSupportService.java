package org.example.aispringboot.AiService;

import com.networknt.schema.SchemaLocation;
import org.example.aispringboot.DTO.command.ConsultationSessionCreateDTO;
import org.example.aispringboot.DTO.response.ConsultationMessageResponseDTO;
import org.example.aispringboot.entity.ConsultationSession;
import org.example.aispringboot.service.ConsultationMessageService;
import org.example.aispringboot.service.ConsultationSessionService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class PsychologicalSupportService {

    @Autowired
    @Qualifier("open-ai")
    private ChatClient chatClient;

    @Autowired
    private ChatMemory chatMemory;

    @Autowired
    private ConsultationSessionService consultationSessionService;

    @Autowired
    private ConsultationMessageService consultationMessageService;

    public StructOutPut.StreamChatSession startSession(Long userId, ConsultationSessionCreateDTO createDTO){
        //创建数据库会话记录
        ConsultationSession consultationSession = consultationSessionService.createSession(userId,createDTO);

        //将初始用户信息保存到Message表
        consultationMessageService.saveUserMessage(consultationSession.getId(),createDTO.getInitialMessage(),null);
        //返回到controller层，创建会话信息
        String sessionId = "session_" + consultationSession.getId();
        return new StructOutPut.StreamChatSession(
                sessionId,
                userId,
                createDTO.getInitialMessage(),
                System.currentTimeMillis(),
                System.currentTimeMillis() + 86400000L,
                1,
                "ACTIVE"

        );


    }

    public Flux<String> streamPsychologicalChat(String sessionId,String userMessage){
        //创建响应流
        return Flux.create(sink ->{
            //sink.next("data")//发布数据
            //sink.complete();//完成
            //sink.error(e)
            Long dbSessionId = extractSessionid(sessionId);
            if(dbSessionId == null){
                sink.error(new RuntimeException("会话id格式错误"));

            }
            //是否为初始消息
            boolean isInitMessage = true;
            //判断是否为初始消息,避免重复保存
            Integer messageCount = consultationMessageService.getMessageCountBySession(dbSessionId);
            if(messageCount == 1){
                ConsultationMessageResponseDTO lastMessage =consultationMessageService.getLastMessasgeBySessionid(dbSessionId);
                if(lastMessage != null && lastMessage.getSenderType() == 1 && userMessage.equals(lastMessage.getContent())){
                    isInitMessage = false;


                }

            }
            if(!isInitMessage){
                consultationMessageService.saveUserMessage(dbSessionId,userMessage,null);
            }

            //创建流式对话
            //生成对话记忆管理
            String conversationid ="conversation_"+sessionId;
            //构建系统提示词
            List<Message> userMessages = new ArrayList<>();
            userMessages.add(new UserMessage(userMessage));
            chatMemory.add(conversationid, userMessages);
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(PromptManage.PSYCHOLOGICAL_SUPPORT_SYSTEM_PROMPT)
            ));
            //用于存储ai完整的响应
            StringBuilder fullResponse = new StringBuilder();
            //使用chatclient发送到Open AI
            chatClient.prompt(prompt)
                            .user(userMessage)
                                    .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID,conversationid))
                    .stream()
                    .content()
                    .doOnNext(Fragment -> {
                        fullResponse.append(Fragment);
                        sink.next(Fragment);
                    })
                    .doOnComplete(()->{
                         String completeRes = fullResponse.toString();
                         //将AI返回的内容保存在数据库
                        consultationMessageService.saveAiMessage(dbSessionId,completeRes,"openai");
                        //添加AI回复到chatMemory
                        List<Message> aiMessage = new ArrayList<>();
                        aiMessage.add(new AssistantMessage(completeRes));
                        chatMemory.add(conversationid,aiMessage);

                        sink.complete();

                    })
                    .doOnError(error ->{
                        sink.error(error);
                    })
                    .subscribe();


        } );
    }

    //用于获取参数中的sessionId
    public Long extractSessionid(String sessionid){
        if(sessionid != null && sessionid.startsWith("session_")){
            String idStr = sessionid.substring("session_".length());
            return Long.parseLong(idStr);
        }
        return null;
    }
}
