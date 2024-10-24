package hack92024.aicopilots.bikerepair.assistant.openai;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.azure.AzureOpenAiChatModel;
import dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel;
import dev.langchain4j.model.azure.AzureOpenAiTokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import hack92024.aicopilots.bikerepair.assistant.openai.service.BikeReparationAgent;
import hack92024.aicopilots.bikerepair.assistant.openai.service.DocIngestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.util.List;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static java.lang.StringTemplate.STR;

@Configuration
public class Config {
    @Value("${doc.names}")
    private String docNames;

    @Bean
    StreamingChatLanguageModel streamingChatLanguageModel() {
        return AzureOpenAiStreamingChatModel.builder()
                .endpoint("https://hack9-2024-openai.openai.azure.com")
                .apiKey("2c79c27ec9da415097f448cfff399653")
                .deploymentName("gpt-4o-4Hack9").build();
    }

    @Bean
    ChatLanguageModel chatLanguageModel() {
        return AzureOpenAiChatModel.builder()
                .endpoint("https://hack9-2024-openai.openai.azure.com")
                .apiKey("2c79c27ec9da415097f448cfff399653")
                .deploymentName("gpt-4o-4Hack9").build();
    }

    @Bean
    BikeReparationAgent bikeReparationAgent(StreamingChatLanguageModel streamingChatLanguageModel,
                                            ChatLanguageModel chatLanguageModel,//){//,
                                            ContentRetriever contentRetriever){//,
//                                            ChatService chatService) {
        return AiServices.builder(BikeReparationAgent.class)
                .streamingChatLanguageModel(streamingChatLanguageModel)
                .chatLanguageModel(chatLanguageModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
//                    .tools(chatService)
                .contentRetriever(contentRetriever)
                .build();
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // You will need to adjust these parameters to find the optimal setting, which will depend on two main factors:
        // - The nature of your data
        // - The embedding model you are using
        int maxResults = 1;
        double minScore = 0.6;

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, ResourceLoader resourceLoader) throws IOException {

        // Normally, you would already have your embedding store filled with your data.
        // However, for the purpose of this demonstration, we will:

        // 1. Create an in-memory embedding store
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. Load an example document ("Miles of Smiles" terms of use)
        var docList = List.of(docNames.split(","));
        docList.forEach(doc -> {
            Resource resource = resourceLoader.getResource(STR."classpath:\{doc}");

            try {
                loadEmbeddingForDocument(embeddingModel, resource, embeddingStore);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return embeddingStore;
    }

    @Bean
    EmbeddingStoreIngestor embeddingStoreIngestor(EmbeddingModel embeddingModel, EmbeddingStore<TextSegment> embeddingStore) {
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, new AzureOpenAiTokenizer("gpt-4o-4Hack9"));
        return EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
    }

    @Bean
    FileSystemResourceLoader resourceLoader() {
        return new FileSystemResourceLoader();
    }

    @Bean
    DocIngestionService docIngestionService(EmbeddingStoreIngestor embeddingStoreIngestor, FileSystemResourceLoader resourceLoader) {
        return new DocIngestionService(embeddingStoreIngestor, resourceLoader);
    }

    private DocumentParser getParserForDocument(Resource resource) throws IOException {
        return resource.getFile().toPath().toString().endsWith(".pdf") ? new ApachePdfBoxDocumentParser() : new TextDocumentParser();
    }

    private void loadEmbeddingForDocument(EmbeddingModel embeddingModel, Resource resource, EmbeddingStore<TextSegment> embeddingStore) throws IOException {
        Document document = loadDocument(resource.getFile().toPath(), getParserForDocument(resource));
        // 3. Split the document into segments 100 tokens each/
        // 4. Convert segments into embeddings
        // 5. Store embeddings into embedding store
        // All this can be done manually, but we will use EmbeddingStoreIngestor to automate this:
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, new AzureOpenAiTokenizer("gpt-4o-4Hack9"));
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);
    }
}