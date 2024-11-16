package com.idenys.fileviewer;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@RestController
public class FilesController {

    private static final Logger log = LoggerFactory.getLogger(FilesController.class);
    private static final String basePath = "/home/ivandenysenko/Downloads";

    private static final String page =
            """
                    <!DOCTYPE html>
                    <html>
                    <title>Files</title>
                    <body>
                       <h2>Files:</h2>
                       %s
                    </body>
                    </html> 
                    """;

    private static final String itemFragment =
            """
                    <div> <a href="%s">%s</a></div>
                    """;

    @GetMapping("/files/**")
    public ResponseEntity<?> files(HttpServletRequest httpServletRequest) throws IOException {
        String servletPath = httpServletRequest.getServletPath();
        log.info("servletRequest: servletPath={}", servletPath);
        Path path = Path.of(basePath + servletPath);
        if (Files.isDirectory(path)) {
            return directoryResponse(path, servletPath);
        } else {
            return fileResponse(path);
        }
    }

    private ResponseEntity<Resource> fileResponse(Path filePath) throws IOException {
        FileSystemResource resource = new FileSystemResource(filePath);

        MediaType mediaType = MediaTypeFactory
                .getMediaType(resource)
                .orElse(MediaType.APPLICATION_OCTET_STREAM);

        ContentDisposition disposition = ContentDisposition
                .attachment()
                .filename(resource.getFilename())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(disposition);

        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    private static ResponseEntity<String> directoryResponse(Path rootPath, String servletPath) throws IOException {
        List<String> directories = new ArrayList<>();
        List<String> files = new ArrayList<>();
        try (Stream<Path> stream = Files.list(rootPath)) {
            stream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    directories.add(path.getFileName().toString());
                } else {
                    files.add(path.getFileName().toString());
                }
            });
        }
        List<String> sortedDirectories = directories.stream().sorted().toList();
        List<String> sortedFiles = files.stream().sorted().toList();

        List<String> formatedItems = new ArrayList<>();
        sortedDirectories.forEach(dirName -> {
            formatedItems.add(itemFragment.formatted(servletPath + "/" + dirName, dirName + "/"));
        });
        sortedFiles.forEach(fileName -> {
            formatedItems.add(itemFragment.formatted(servletPath + "/" + fileName, fileName));
        });
        log.debug("Formatted items: {}", formatedItems);

        String formattedItemsString = String.join("\n", formatedItems);
        String resultPage = page.formatted(formattedItemsString);

        return ResponseEntity.ok(resultPage);
    }
}
