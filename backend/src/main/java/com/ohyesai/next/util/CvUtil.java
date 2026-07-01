package com.ohyesai.next.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.IdUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.ohyesai.next.OhyesaiNextServerApplication;
import com.ohyesai.next.common.enums.CodeEnum;
import com.ohyesai.next.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 视觉处理工具类
 */
@Slf4j
public class CvUtil {

    private static String FFMPEG_PATH;

    /**
     * ffmpeg 携带的媒体信息探测工具
     */
    private static String FFPROBE_PATH;

    private static String getFFprobePath() {
        if (FFPROBE_PATH == null) {
            if (OhyesaiNextServerApplication.applicationContext == null) {
                FFPROBE_PATH = "ffprobe";
            } else {
                FFPROBE_PATH = OhyesaiNextServerApplication.applicationContext.getEnvironment().resolvePlaceholders("${ffprobe-path:ffprobe}");
            }
        }
        return FFPROBE_PATH;
    }

    private static String getFFmpegPath() {
        if (FFMPEG_PATH == null) {
            if (OhyesaiNextServerApplication.applicationContext == null) {
                FFMPEG_PATH = "ffmpeg";
            } else {
                FFMPEG_PATH = OhyesaiNextServerApplication.applicationContext.getEnvironment().resolvePlaceholders("${ffmpeg-path:ffmpeg}");
            }
        }
        return FFMPEG_PATH;
    }

    /**
     * 视频转gif
     *
     * @param videoFile 视频文件
     * @param gifFile   gif文件 文件扩展名为gif
     * @param setPts    设置帧时间戳(视频调速)
     */
    public static void video2Gif(File videoFile, File gifFile, Double setPts) {
        // 240*240 是微信推荐表情大小
        String vf = "fps=15,scale=240:-1:flags=lanczos";
        if (setPts != null) {
            vf = "setpts=" + setPts + "*PTS," + vf;
        }

        // ffmpeg -y -i 94db34b1-6693-43f0-b9e6-e440f9467a4b.mp4 -vf "fps=15,scale=240:-1:flags=lanczos" -c:v gif out.gif
        List<String> cmd = List.of(
                getFFmpegPath(),
                "-y",
                "-i", videoFile.getAbsolutePath(),
                "-vf", vf,
                "-c:v", "gif",
                gifFile.getAbsolutePath()
        );
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process);
            int exitCode = process.waitFor();
            log.debug("video2Gif exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("video2Gif 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "视频转gif失败");
            }
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 音频淡出
     *
     * @param start    淡出的开始时间
     * @param duration 淡出的持续时间
     */
    public static void fadeOutAudio(File audioFile, File outputFile, int start, int duration) {
        List<String> cmd = List.of(
                getFFmpegPath(),
                "-i", audioFile.getAbsolutePath(),
                "-af", "afade=t=out:st=" + start + ":d=" + duration,
                "-c:v", "copy",
                "-c:a", "aac",
                "-avoid_negative_ts", "make_zero",
                outputFile.getAbsolutePath()
        );

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process); // 打印错误信息
            int exitCode = process.waitFor();
            log.debug("fadeOutAudio exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("fadeOutAudio 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "视频合并失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 通过 ffmpeg 合并多个视频文件
     * 注意：不包含音频
     *
     * @param filePaths  输入的视频文件路径列表
     * @param outputFile 输出的合并后视频文件
     * @throws BusinessException 如果合并失败则抛出业务异常
     */
    public static void concatVideo(List<File> filePaths, File outputFile) {
        // 构建 ffmpeg 命令参数
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegPath()); // 获取 ffmpeg 路径

        // 添加输入文件
        for (File filePath : filePaths) {
            cmd.add("-i");
            cmd.add(filePath.getAbsolutePath());
        }

        // 构建 filter_complex 参数
        StringBuilder concatFilter = new StringBuilder();
        for (int i = 0; i < filePaths.size(); i++) {
            // 只合并视频流
            concatFilter.append(String.format("[%d:v]", i));
        }
        concatFilter.append(String.format("concat=n=%d:v=1[v]", filePaths.size()));

        cmd.add("-filter_complex");
        cmd.add(concatFilter.toString());

        cmd.add("-y");

        cmd.add("-map");
        cmd.add("[v]");

//        cmd.add("-map");
//        cmd.add("0:a?");

        cmd.add("-c:v");
        cmd.add("libx264");

        cmd.add("-level");
        cmd.add("4.1");

//        cmd.add("-c:a");
//        cmd.add("aac");

        cmd.add("-an");

        cmd.add(outputFile.getAbsolutePath());

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process); // 打印错误信息
            int exitCode = process.waitFor();
            log.debug("concatVideo exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("concatVideo 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "视频合并失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 同时拼接音频和视频
     *
     * @param filePaths
     * @param outputFile
     */
    public static void concatVideoAudio(List<File> filePaths, File outputFile) {
        // 构建 ffmpeg 命令参数
        List<String> cmd = new ArrayList<>();
        cmd.add(getFFmpegPath()); // 获取 ffmpeg 路径

        // 添加输入文件
        for (File filePath : filePaths) {
            cmd.add("-i");
            cmd.add(filePath.getAbsolutePath());
        }

        // 构建 filter_complex 参数
        StringBuilder concatFilter = new StringBuilder();
        for (int i = 0; i < filePaths.size(); i++) {
            // 只合并视频流
            concatFilter.append(String.format("[%d:v][%d:a]", i, i));
        }
        concatFilter.append(String.format("concat=n=%d:v=1:a=1[v][a]", filePaths.size()));

        cmd.add("-filter_complex");
        cmd.add(concatFilter.toString());

        cmd.add("-y");

        cmd.add("-map");
        cmd.add("[v]");

        cmd.add("-map");
        cmd.add("[a]");

        cmd.add("-c:v");
        cmd.add("libx264");

        cmd.add("-level");
        cmd.add("4.1");

        cmd.add("-c:a");
        cmd.add("aac");

        cmd.add(outputFile.getAbsolutePath());

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process); // 打印错误信息
            int exitCode = process.waitFor();
            log.debug("concatVideo exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("concatVideo 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "视频合并失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 通过 ffmpeg 合并音频和视频
     *
     * @param videoFile  输入的视频文件路径
     * @param audioFile  输入的音频文件路径
     * @param outputFile 输出的合并后文件路径
     * @throws BusinessException 如果合并失败则抛出业务异常
     */
    public static void mergeAudioVideo(File videoFile, File audioFile, File outputFile) {
        // 构建 ffmpeg 命令参数
        List<String> cmd = List.of(
                getFFmpegPath(), // 获取 ffmpeg 路径
                "-i", videoFile.getAbsolutePath(),
                "-i", audioFile.getAbsolutePath(),
                "-c:v", "libx264", "-level", "4.1",
                "-c:a", "aac",
//                "-bsf:v", "h264_metadata=level=4.1",
                "-shortest",
                outputFile.getAbsolutePath()
        );

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process); // 打印错误信息
            int exitCode = process.waitFor();
            log.debug("mergeAudioVideo exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("mergeAudioVideo 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "音频和视频合并失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 通过 ffmpeg 将字幕烧录到视频
     *
     * @param videoFile  输入的视频文件
     * @param srtFile    输入的 SRT 字幕文件
     * @param outputFile 输出的视频文件
     */
    public static void addSubtitles(File videoFile, File srtFile, File outputFile, int fontSize) {
        String filter = String.format("subtitles='%s':force_style='FontName=Lantinghei SC,FontSize=%d,WrapStyle=0'", srtFile.getAbsolutePath(), fontSize);
        List<String> cmd = List.of(
                getFFmpegPath(),
                "-i", videoFile.getAbsolutePath(),
                "-vf", filter,
                "-c:v", "libx264", "-level", "4.1",
                "-c:a", "aac",
                "-y",
                outputFile.getAbsolutePath()
        );

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process);
            int exitCode = process.waitFor();
            log.debug("addSubtitles exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("addSubtitles 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "字幕添加失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 添加图片水印
     *
     * @param videoFile
     * @param watermarkFile
     * @param outputFile
     */
    public static void watermark(File videoFile, File watermarkFile, File outputFile) {
        // ffmpeg -i bilibili-日语.mp4 -i 水印@1x.png -filter_complex "overlay=10:10" output.mp4
        List<String> cmd = List.of(
                getFFmpegPath(),
                "-i", videoFile.getAbsolutePath(),
                "-i", watermarkFile.getAbsolutePath(),
                "-filter_complex", "overlay=10:10",
                "-c:v", "libx264", "-level", "4.1",
                "-c:a", "aac",
                outputFile.getAbsolutePath()
        );

        // 执行命令
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process); // 打印错误信息
            int exitCode = process.waitFor();
            log.debug("watermark exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("watermark 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "水印添加失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 截取音频
     * 单位/秒
     *
     */
    public static void trimAudio(File audioFile, File outputFile, double start, double duration) {
        String ss = String.format("%.3f", start);
        String t = String.format("%.3f", duration);

        List<String> cmd = List.of(
                getFFmpegPath(),
                "-i", audioFile.getAbsolutePath(),
                "-ss", ss,
                "-t", t,
                "-c:a", "pcm_s16le",
                "-y",
                outputFile.getAbsolutePath()
        );
        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process);
            int exitCode = process.waitFor();
            log.debug("trimAudio exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("trimAudio 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "音频截取失败");
            }
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 通过 ffmpeg 截断视频到指定时长（秒）。
     * <p>
     *
     * @param videoFile       输入视频文件
     * @param durationSeconds 目标时长（秒，如 4.350）
     * @param outputFile      输出文件
     */
    public static void trimVideo(File videoFile, File outputFile, double start, double durationSeconds) {
        String ss = String.format("%.3f", start);
        String durationStr = String.format("%.3f", durationSeconds);

        List<String> cmd = List.of(
                getFFmpegPath(),
                "-i", videoFile.getAbsolutePath(),
                "-ss", ss,
                "-t", durationStr,
                "-c:v", "libx264", "-level", "4.1",
                "-c:a", "aac",
                "-y",
                outputFile.getAbsolutePath()
        );

        ProcessBuilder processBuilder = new ProcessBuilder(cmd);
        try {
            Process process = processBuilder.start();
            printProcessErr(process);
            int exitCode = process.waitFor();
            log.debug("trimVideo exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("trimVideo 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "视频截断失败");
            }
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }


    public static MediaInfo mediaInfo(File filePath) {
        return mediaInfo(filePath, true);
    }

    /**
     * 通过 ffmpeg 获取媒体文件信息
     *
     * @param filePath 文件路径
     * @param cover    是否需要生成封面
     * @return 封面文件与视频输入文件在同一个目录下
     */
    public static MediaInfo mediaInfo(File filePath, boolean cover) {

        try {
            // 构建 ffprobe 命令参数
            List<String> cmd = List.of(
                    getFFprobePath(), // 获取 ffprobe 路径
                    "-v", "quiet",
                    "-print_format", "json",
                    "-show_format",
                    "-show_streams",
                    filePath.getAbsolutePath()
            );

            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            Process process = processBuilder.start();
            printProcessErr(process);
            String output = printProcessInput(process);
            int exitCode = process.waitFor();
            log.debug("mediaInfo exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("mediaInfo 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "获取元信息失败");
            }

            // 解析 JSON 输出
            JsonNode jsonNode = JsonUtil.readTree(output);
            String durationStr = jsonNode.path("format").path("duration").asText();
            double durationSeconds = Double.parseDouble(durationStr);


            // 首帧封面
            File coverFile = null;
            Integer width = null, height = null;
            // 找到视频流
            Optional<JsonNode> videoStream = jsonNode.withArrayProperty("streams").valueStream().filter(node -> node.path("codec_type").asText().equals("video")).findFirst();
            if (videoStream.isPresent()) {
                // 提取宽高
                width = videoStream.get().path("width").asInt();
                height = videoStream.get().path("height").asInt();

                // 提取首帧
                if (cover) {
                    coverFile = new File(filePath.getParentFile(), "cover_" + IdUtil.fastSimpleUUID() + ".jpg");
                    List<String> coverCmd = List.of(
                            getFFmpegPath(),
                            "-i", filePath.getAbsolutePath(),
                            "-frames:v", "1",
                            "-q:v", "2",
                            "-y",
                            coverFile.getAbsolutePath()
                    );

                    processBuilder = new ProcessBuilder(coverCmd);
                    process = processBuilder.start();
                    printProcessErr(process);
                    exitCode = process.waitFor();
                    log.debug("mediaInfo cover exitCode: {}", exitCode);
                    if (exitCode != 0) {
                        log.error("mediaInfo cover 错误  命令行: {}", cmd);
                        throw new BusinessException(CodeEnum.Unknow, "获取首帧失败");
                    }
                }
            }
            return new MediaInfo(width, height, Duration.ofMillis((long) (durationSeconds * 1000)), coverFile);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 规范化输入视频
     * 缩放视频到指定像素  补充黑边
     * 如果视频无音轨则自动补充静音轨
     *
     * @param videoFile
     * @param outputFile
     * @param targetWidth
     * @param targetHeight
     */
    public static void scale(File videoFile, File outputFile, int targetWidth, int targetHeight) {
        try {
            MediaInfo mediaInfo = mediaInfo(videoFile, false);
            // 原始宽高
            int originalWidth = mediaInfo.width();
            int originalHeight = mediaInfo.height();

            // 所有格式都符合 则跳过
            if (targetWidth == originalWidth && targetHeight == originalHeight) {
                log.info("scale 视频与原比例相同无需处理");
                FileInputStream fis = new FileInputStream(videoFile);
                FileOutputStream fos = new FileOutputStream(outputFile);
                IoUtil.copy(fis, fos);
                return;
            }

            List<String> cmd = List.of(
                    getFFmpegPath(),
                    "-y",
                    "-i", videoFile.getAbsolutePath(),
                    "-vf", String.format("scale=%d:%d:force_original_aspect_ratio=decrease,setsar=1:1,pad=%d:%d:(ow-iw)/2:(oh-ih)/2:black", targetWidth, targetHeight, targetWidth, targetHeight),
                    "-c:v", "libx264", "-level", "4.1",
                    "-c:a", "aac",
                    outputFile.getAbsolutePath()
            );

            ProcessBuilder pb = new ProcessBuilder(cmd);
            Process process = pb.start();
            printProcessErr(process);
            int exitCode = process.waitFor();
            log.debug("scaleV2 exitCode: {}", exitCode);
            if (exitCode != 0) {
                log.error("scaleV2 错误  命令行: {}", cmd);
                throw new BusinessException(CodeEnum.Unknow, "缩放视频失败");
            }

        } catch (Exception e) {
            throw new BusinessException(e);
        }
    }


    /**
     * 打印错误信息
     *
     * @param process
     */
    public static void printProcessErr(Process process) {
        try (BufferedReader br = process.errorReader()) {
            String line;
            while ((line = br.readLine()) != null) {
                log.info("{}", line);
            }
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }

    private static String printProcessInput(Process process) {
        try (BufferedReader br = process.inputReader()) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            throw new BusinessException(e);
        }
    }


    public record MediaInfo(
            Integer width,
            Integer height,
            Duration duration,
            File cover
    ) {
    }

}


