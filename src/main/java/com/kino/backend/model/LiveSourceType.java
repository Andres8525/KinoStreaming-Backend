package com.kino.backend.model;

/**
 * Where the video of a live broadcast comes from.
 *
 * <ul>
 *   <li>{@code CAMERA}: the creator's browser captures the camera and sends it to each viewer over WebRTC;
 *   Kino only relays the connection setup (see {@code LiveSignalingService}).</li>
 *   <li>{@code HLS}: the creator streams with OBS to an external media service and registers its public
 *   {@code .m3u8} playback URL.</li>
 * </ul>
 */
public enum LiveSourceType {
    CAMERA,
    HLS
}
