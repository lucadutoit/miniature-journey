package com.s15.codectest;

// One of the audio files in app/src/main/assets/audio/. All are the same 60 s clip,
// just encoded with a different codec / bitrate.
public class Clip {
    public final String file;
    public final String codec;
    public final int kbps; // 0 for FLAC (lossless)

    public Clip(String file, String codec, int kbps) {
        this.file = file;
        this.codec = codec;
        this.kbps = kbps;
    }

    public String label() {
        if (kbps == 0) return codec;
        return codec + " " + kbps + "k";
    }

    public static final Clip[] ALL = {
            new Clip("clip_mp3_96.mp3", "MP3", 96),
            new Clip("clip_mp3_128.mp3", "MP3", 128),
            new Clip("clip_mp3_192.mp3", "MP3", 192),
            new Clip("clip_aac_96.m4a", "AAC", 96),
            new Clip("clip_aac_128.m4a", "AAC", 128),
            new Clip("clip_aac_192.m4a", "AAC", 192),
            new Clip("clip_opus_96.ogg", "Opus", 96),
            new Clip("clip_opus_128.ogg", "Opus", 128),
            new Clip("clip_opus_192.ogg", "Opus", 192),
            new Clip("clip_flac.flac", "FLAC", 0),
    };

    public static Clip find(String file) {
        for (Clip c : ALL) {
            if (c.file.equals(file)) return c;
        }
        return null;
    }
}
