package com.myproj.code.utils;

import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.ai.document.Document;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件转换
 */
@Component
public class FileToDocuments {

    public List<Document> handle(MultipartFile file) throws IOException {
        //将不同文件转化为Document
        FileHandler classifier = classifier(file);
        return classifier.run(file);
    }

    private FileHandler classifier(MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();

        return switch (extension) {
            case "md", "markdown" -> new MarkdownFileHandler();
            case "pdf" -> new PdfFileHandler();
            case "txt" -> new TextFileHandler();
            default -> throw new IllegalArgumentException("不支持的文件类型: " + extension);
        };
    }


    public interface FileHandler {
        List<Document> run(MultipartFile file) throws IOException;
    }

    // 针对不同的文件读取器，是实现不同的方法
    private static class MarkdownFileHandler implements FileHandler {
        @Override
        public List<Document> run(MultipartFile file) throws IOException {
            MarkdownDocumentReaderConfig readerConfig = MarkdownDocumentReaderConfig.builder()
                    .withHorizontalRuleCreateDocument(false) // 禁止“--”为分割线去分割document
                    .withIncludeCodeBlock(false) //禁止以代码块分割document
                    .build();
            Resource byteArrayResource = new ByteArrayResource(file.getBytes());
            MarkdownDocumentReader reader = new MarkdownDocumentReader(byteArrayResource, readerConfig);
            // 切分文档
            return ChineseTokenTextSplitter.quicklyBuilder().split(reader.read());
        }
    }

    private static class PdfFileHandler implements FileHandler {
        @Override
        public List<Document> run(MultipartFile file) throws IOException {
            List<Document> read = new PagePdfDocumentReader(new ByteArrayResource(file.getBytes())).read();
            // 切分文档
            read = ChineseTokenTextSplitter.quicklyBuilder().split(read);
            return read;
        }
    }

    private static class TextFileHandler implements FileHandler {
        @Override
        public List<Document> run(MultipartFile file) throws IOException {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
                StringBuilder content = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line);
                }
                List<Document> documents = new ArrayList<>(1);
                documents.add(new Document(content.toString()));
                // 切分文档
                documents = ChineseTokenTextSplitter.quicklyBuilder().split(documents);
                return documents;
            } catch (IOException e) {
                throw new RuntimeException("读取文本文件失败", e);
            }
        }
    }
}
