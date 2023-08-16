//
// Created by woesss on 01.08.2023.
//

#include <forward_list>
#define TSF_IMPLEMENTATION
#define TML_IMPLEMENTATION
#include "Player.h"
#include "util/log.h"

#define LOG_TAG "MMAPI"

namespace tsf_mmapi {
    tsf *Player::soundBank{};

    Player::Player(tsf *synth, tml_message *midi) : synth(synth), media(midi), currentMsg(midi) {
        unsigned int timeLength = 0;
        tml_get_info(midi, nullptr, nullptr, nullptr, nullptr, &timeLength);
        duration = timeLength * 1000LL;
        //Initialize preset on special 10th MIDI channel to use percussion sound bank (128) if available
        tsf_channel_set_bank_preset(synth, 9, 128, 0);
    }

    Player::~Player() {
        close();
        delete playerListener;
    }

    bool Player::createPlayer(const char *path, Player **pPlayer) {
        tsf *synth = tsf_copy(soundBank);
        if (synth == nullptr) {
            return false;
        }
        tml_message *midi = tml_load_filename(path);
        if (midi == nullptr) {
            return false;
        }
        *pPlayer = new tsf_mmapi::Player(synth, midi);
        return true;
    }

    oboe::Result Player::createAudioStream() {
        oboe::AudioStreamBuilder builder;
        builder.setDirection(oboe::Direction::Output);
        builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
        builder.setSharingMode(oboe::SharingMode::Shared);
        builder.setFormat(oboe::AudioFormat::Float);
        builder.setChannelCount(2);
        builder.setCallback(this);

        oboe::Result result = builder.openStream(oboeStream);
        if (result != oboe::Result::OK) {
            oboeStream.reset();
            ALOGE("%s: can't open audio stream. %s", __func__, convertToText(result));
            return result;
        }

        // Set the SoundFont rendering output mode
        tsf_set_output(synth, TSF_STEREO_INTERLEAVED, oboeStream->getSampleRate(), 0.0f);
        tsf_set_volume(synth, computeGain(volume));
        return result;
    }

    oboe::Result Player::prefetch() {
        oboe::Result result = createAudioStream();
        if (result == oboe::Result::OK) {
            state = PREFETCHED;
        }
        return result;
    }

    oboe::Result Player::start() {
        oboe::Result result = oboeStream->start();
        if (result != oboe::Result::OK) {
            ALOGE("%s: can't start audio stream. %s", __func__, oboe::convertToText(result));
            return result;
        }
        state = STARTED;
        return result;
    }

    oboe::Result Player::pause() {
        if (oboeStream->getState() < oboe::StreamState::Starting ||
            oboeStream->getState() > oboe::StreamState::Started) {
            return oboe::Result::OK;
        }
        oboe::Result result = oboeStream->pause();
        if (result != oboe::Result::OK) {
            return result;
        }
        state = PREFETCHED;
        return result;
    }

    void Player::deallocate() {
        oboeStream->stop();
        oboeStream->close();
        oboeStream.reset();
        playTime = 0LL;
        tsf_reset(synth);
        loopCount = looping;
        currentMsg = media;
        state = REALIZED;
    }

    void Player::close() {
        if (state == CLOSED) {
            return;
        } else if (state == STARTED) {
            oboeStream->stop();
        }
        if (state >= PREFETCHED) {
            oboeStream->close();
            oboeStream.reset();
        }
        tml_free(media);
        tsf_close(synth);
        media = nullptr;
        synth = nullptr;
        state = CLOSED;
    }

    int64_t Player::setMediaTime(int64_t now) {
        if (now < 0) {
            now = 0;
        } else if (now > duration) {
            now = duration;
        }
        timeToSet = now;
        return now;
    }

    int64_t Player::getMediaTime() const {
        return playTime;
    }

    void Player::setRepeat(int32_t count) {
        if (count > 0) {
            count--;
        }
        looping = count;
        loopCount = count;
    }

    int32_t Player::setPan(int32_t pan) {
        return 0;
    }

    int32_t Player::getPan() {
        return 0;
    }

    void Player::setMute(bool mute) {
        if (mute && !muted) {
            tsf_set_volume(synth, 0.0f);
            muted = true;
        } else if (!mute && muted) {
            tsf_set_volume(synth, computeGain(volume));
            muted = false;
        }
    }

    int32_t Player::setVolume(int32_t level) {
        volume = level;
        tsf_set_volume(synth, computeGain(level));
        return volume;
    }

    bool Player::isMuted() const {
        return muted;
    }

    int32_t Player::getVolume() const {
        return volume;
    }

    bool Player::realize() {
        state = REALIZED;
        return true;
    }

    void Player::setListener(PlayerListener *listener) {
        this->playerListener = listener;
    }

    bool Player::initSoundBank(const char *sound_bank) {
        tsf *synth = tsf_load_filename(sound_bank);
        if (synth == nullptr) {
            return false;
        }
        tsf_mmapi::Player::soundBank = synth;
        return true;
    }

    float Player::computeGain(int32_t level) {
        return static_cast<float>(1.0 - log(101 - level) / log(101));
    }

    void Player::processEvents(bool playMode) {
        std::forward_list<tml_message *> notes;
        for (; currentMsg && playTime >= currentMsg->time * 1000LL; currentMsg = currentMsg->next) {
            switch (currentMsg->type) {
                case TML_PROGRAM_CHANGE: //channel program (preset) change (special handling for 10th MIDI channel with drums)
                    tsf_channel_set_presetnumber(synth, currentMsg->channel,
                                                 currentMsg->program,
                                                 (currentMsg->channel == 9));
                    break;
                case TML_NOTE_ON: //play a note
                    if (playMode) {
                        tsf_channel_note_on(synth, currentMsg->channel, currentMsg->key,
                                            static_cast<float>(currentMsg->velocity) / 127.0f);
                    } else {
                        notes.push_front(currentMsg);
                    }
                    break;
                case TML_NOTE_OFF: //stop a note
                    if (!playMode) {
                        notes.remove_if([this](tml_message *m) {
                            return m->channel == currentMsg->channel && m->key == currentMsg->key;
                        });
                    }
                    tsf_channel_note_off(synth, currentMsg->channel, currentMsg->key);
                    break;
                case TML_PITCH_BEND: //pitch wheel modification
                    tsf_channel_set_pitchwheel(synth, currentMsg->channel, currentMsg->pitch_bend);
                    break;
                case TML_CONTROL_CHANGE: //MIDI controller messages
                    tsf_channel_midi_control(synth, currentMsg->channel, currentMsg->control,
                                             currentMsg->control_value);
                    break;
            }
        }
        if (!playMode) {
            for (const auto &m: notes) {
                tsf_channel_note_on(synth, m->channel, m->key, static_cast<float>(m->velocity) / 127.0f);
            }
            notes.clear();
        }
    }

    oboe::DataCallbackResult
    Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
        if (currentMsg == nullptr) {
            playerListener->postEvent(END_OF_MEDIA, playTime);
            if (looping == -1 || (--loopCount) > 0) {
                currentMsg = media;
                playTime = 0;
                tsf_reset(synth);
                playerListener->postEvent(START, playTime);
            } else {
                return oboe::DataCallbackResult::Stop;
            }
        }
        if (timeToSet != -1) {
            if (timeToSet < playTime) {
                tsf_reset(synth);
                currentMsg = media;
            }
            playTime = timeToSet;
            timeToSet = -1;
            processEvents(false);
        }
        //Number of samples to process
        int sampleBlock = TSF_RENDER_EFFECTSAMPLEBLOCK;
        auto *stream = static_cast<float *>(audioData);
        for (; numFrames > 0; numFrames -= sampleBlock, stream += sampleBlock * audioStream->getChannelCount()) {
            //We progress the MIDI playback and then process TSF_RENDER_EFFECTSAMPLEBLOCK samples at once
            if (sampleBlock > numFrames) sampleBlock = numFrames;

            //Loop through all MIDI messages which need to be played up until the current playback time
            playTime += sampleBlock * 1000000LL / audioStream->getSampleRate();
            processEvents(true);

            // Render the block of audio samples in float format
            tsf_render_float(synth, stream, sampleBlock, 0);
        }
        return oboe::DataCallbackResult::Continue;
    }

    void Player::onErrorAfterClose(oboe::AudioStream *stream, oboe::Result result) {
        if (result == oboe::Result::ErrorDisconnected) {
            oboe::Result res = createAudioStream();
            if (res != oboe::Result::OK) {
                ALOGE("%s: reconnect error=%s", __func__, oboe::convertToText(res));
                playerListener->postEvent(ERROR, 0);
            } else if (state == STARTED) {
                oboeStream->requestStart();
            }
        } else {
            ALOGE("%s: %s", __func__, oboe::convertToText(result));
            playerListener->postEvent(ERROR, 0);
        }
    }
} // tsf_mmapi
