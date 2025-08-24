package de.calisto.srttranslator.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

  public static final String SYSTEM_PROMPT =
      "You are a professional translator who translates subtitles from %s into %s. The translation should be natural and fluent, while preserving the original meaning and style. Do not use formatting, code markup, or quotation marks in the translation. If there are choices, choose on and only return this. Do not explain the choice or ask questions.";
  public static final String PROMPT_CONTEXT = "Context for better translation: ";
  public static final String PROMPT_TO_BE_TRANSLATED = "Sentence to be translated: ";

  private record SubtitleEntry(int number, String timeCode, String orgText, String transText) {}

  @Value("${directories.input}")
  @NonNull
  private String dirInput;

  @Value("${directories.output}")
  @NonNull
  private String dirOutput;

  @Value("${prompt.add-to-system}")
  @NonNull
  private String addToSystemPrompt;

  @Value("${output.replace-string}")
  @NonNull
  private String outputReplaceString;

  @Value("${languages.input}")
  @NonNull
  private String inputLanguage;

  @Value("${languages.output}")
  @NonNull
  private String outputLanguage;

  private final ChatClient chatClient;

  @Autowired
  public TranslationService(ChatClient.Builder chatClientBuilder) {
    this.chatClient = chatClientBuilder.build();
  }

  public void translate() {
    try {
      List<Path> srtFiles;
      try (var pathStream = Files.list(Paths.get(dirInput))) {
        srtFiles =
            pathStream.filter(path -> path.toString().toLowerCase().endsWith(".srt")).toList();
      }

      for (Path srtFile : srtFiles) {
        System.out.println("Processing file: " + srtFile.toFile().getAbsolutePath());
        List<SubtitleEntry> subtitleEntries = new ArrayList<>();
        List<String> lines = Files.readAllLines(srtFile);

        int i = 0;
        while (i < lines.size()) {
          if (lines.get(i).trim().isEmpty()) {
            i++;
            continue;
          }

          int number = Integer.parseInt(lines.get(i));
          String timeCode = lines.get(i + 1);

          StringBuilder text = new StringBuilder();
          i += 2;
          while (i < lines.size() && !lines.get(i).trim().isEmpty()) {
            text.append(lines.get(i)).append("\n");
            i++;
          }

          subtitleEntries.add(new SubtitleEntry(number, timeCode, text.toString().trim(), ""));
        }

        System.out.println(
            "Read file: "
                + srtFile.getFileName()
                + " with "
                + subtitleEntries.size()
                + " subtitleEntries");

        translateEntries(subtitleEntries);

        writeSrtFile(srtFile, subtitleEntries);
        System.out.println("Finished: " + srtFile.getFileName());
      }
    } catch (IOException e) {
      throw new RuntimeException("Error reading SRT files", e);
    }
  }

  private void translateEntries(List<SubtitleEntry> subtitleEntries) {
    //    for (var subtitleIndex = 0; subtitleIndex < 10; subtitleIndex++) {
    for (var subtitleIndex = 0; subtitleIndex < subtitleEntries.size(); subtitleIndex++) {
      System.out.println("Processing sentence " + subtitleIndex + ":");
      var context = new StringBuilder();
      for (var index = 1; index <= 4; index++) {
        if (subtitleIndex - index >= 0) {
          context
              .append("Previous sentence ")
              .append(index)
              .append(": ")
              .append(subtitleEntries.get(subtitleIndex - index).orgText())
              .append("\n");
        }
      }

      for (var index = 1; index <= 4; index++) {
        if (subtitleIndex + index < subtitleEntries.size()) {
          context
              .append("Next sentence ")
              .append(index)
              .append(": ")
              .append(subtitleEntries.get(subtitleIndex + index).orgText())
              .append("\n");
        }
      }

      try {
        String translation =
            chatClient
                .prompt()
                .system(
                    String.format(SYSTEM_PROMPT, inputLanguage, outputLanguage)
                        + addToSystemPrompt
                        + "\n\n"
                        + PROMPT_CONTEXT
                        + "\n"
                        + context
                        + "\n")
                .user(PROMPT_TO_BE_TRANSLATED + subtitleEntries.get(subtitleIndex).orgText())
                .call()
                .content();
        var updatedEntry =
            new SubtitleEntry(
                subtitleEntries.get(subtitleIndex).number(),
                subtitleEntries.get(subtitleIndex).timeCode(),
                subtitleEntries.get(subtitleIndex).orgText(),
                translation);
        subtitleEntries.set(subtitleIndex, updatedEntry);
      } catch (Exception e) {
        throw new RuntimeException("Error calling AI service", e);
      }
    }
  }

  private void writeSrtFile(Path srtFile, List<SubtitleEntry> subtitleEntries) {
    Path outputPath =
        Paths.get(dirOutput, srtFile.getFileName().toString().replace(outputReplaceString, ""));
    List<String> outputLines = new ArrayList<>();
    for (SubtitleEntry entry : subtitleEntries) {
      outputLines.add(String.valueOf(entry.number()));
      outputLines.add(entry.timeCode());
      outputLines.add(entry.transText());
      outputLines.add("");
    }
    try {
      Files.write(outputPath, outputLines);
    } catch (IOException e) {
      throw new RuntimeException("Error writing SRT file", e);
    }
  }
}
