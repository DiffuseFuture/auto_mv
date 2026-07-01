package com.ohyesai.next.biz.vio.bo;

import com.ohyesai.next.biz.vio.entity.VioProject;

import java.time.Duration;

public record DoMergeAudioVideoResp(VioProject vioProject, Duration duration) {
}
