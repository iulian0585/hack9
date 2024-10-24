package hack92024.aicopilots.bikerepair.assistant.openai.rest;

import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.service.TokenStream;
import hack92024.aicopilots.bikerepair.assistant.openai.model.Prompt;
import hack92024.aicopilots.bikerepair.assistant.openai.service.BikeReparationAgent;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@CrossOrigin(origins = "*")
public class ChatController {
    private final BikeReparationAgent agent;
//    ChatLanguageModel chatLanguageModel;
//    StreamingChatLanguageModel streamingChatLanguageModel;
//    public ChatController(ChatLanguageModel chatLanguageModel, StreamingChatLanguageModel streamingChatLanguageModel) {
//        this.chatLanguageModel = chatLanguageModel;
//        this.streamingChatLanguageModel = streamingChatLanguageModel;
//    }

     public ChatController(BikeReparationAgent agent) {
         this.agent = agent;
     }

     @GetMapping("/chat")
     public String chat(@RequestParam("question") String question) {
         return agent.chat(question);
//         return chatLanguageModel.chat(question);
    }

    @GetMapping(value = "/chatstream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @ResponseBody
    public Flux<String> chatStream(@RequestParam("question") String question) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();

        TokenStream ts = agent.chatStream(question);
        ts.onNext(sink::tryEmitNext)
                .onError(sink::tryEmitError)
                .start();

        return sink.asFlux();
    }
}
