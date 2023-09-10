//
// Created by woesss on 01.08.2023.
//


#define TSF_IMPLEMENTATION
#define TML_IMPLEMENTATION

#include <forward_list>
#include "tsf_player.h"
#include "util/log.h"

#define LOG_TAG "MMAPI"
#define NUM_CHANNELS 2

namespace mmapi {
    namespace tiny {
        tsf *Player::soundBank{};

        Player::Player(tsf *synth, tml_message *midi, const int64_t duration)
                : BasePlayer(duration), synth(synth), media(midi), currentMsg(midi) {
            //Initialize preset on special 10th MIDI channel to use percussion sound bank (128) if available
            tsf_channel_set_bank_preset(synth, 9, 128, 0);
        }

        Player::~Player() {
            close();
        }

        int32_t Player::createPlayer(const char *path, Player **pPlayer) {
            tsf *synth = tsf_copy(soundBank);
            if (synth == nullptr) {
                return -1;
            }
            tml_message *midi = tml_load_filename(path);
            if (midi == nullptr) {
                return -2;
            }
            unsigned int timeLength = -1;
            tml_get_info(midi, nullptr, nullptr, nullptr, nullptr, &timeLength);
            *pPlayer = new Player(synth, midi, timeLength * 1000LL);
            return 0;
        }

        oboe::Result Player::createAudioStream() {
            oboe::AudioStreamBuilder builder;
            builder.setDirection(oboe::Direction::Output);
            builder.setPerformanceMode(oboe::PerformanceMode::LowLatency);
            builder.setSharingMode(oboe::SharingMode::Shared);
            builder.setFormat(oboe::AudioFormat::Float);
            builder.setChannelCount(NUM_CHANNELS);
            builder.setCallback(this);
            builder.setFormatConversionAllowed(true);

            oboe::Result result = builder.openStream(oboeStream);
            if (result != oboe::Result::OK) {
                oboeStream.reset();
                ALOGE("%s: can't open audio stream. %s", __func__, oboe::convertToText(result));
                return result;
            }

            // Set the SoundFont rendering output mode
            tsf_set_output(synth, TSF_STEREO_INTERLEAVED, oboeStream->getSampleRate(), 0.0f);
            tsf_set_volume(synth, computeGain(getVolume()));
            return result;
        }

        void Player::deallocate() {
            BasePlayer::deallocate();
            timeToSet = 0;
        }

        void Player::close() {
            BasePlayer::close();
            tml_free(media);
            tsf_close(synth);
            media = nullptr;
            synth = nullptr;
        }

        int64_t Player::getMediaTime() {
            return playTime;
        }

        int32_t Player::setVolume(int32_t level) {
            tsf_set_volume(synth, computeGain(level));
            return BasePlayer::setVolume(level);
        }

        int32_t Player::initSoundBank(const char *sound_bank) {
            tsf *synth = tsf_load_filename(sound_bank);
            if (synth == nullptr) {
                return -1;
            }
            Player::soundBank = synth;
            return 0;
        }

        float Player::computeGain(int32_t level) {
            return static_cast<float>(1.0 - log(101 - level) / log(101));
        }

        void Player::processEvents() {
            //Loop through all MIDI messages which need to be played up until the current playback time
            for (; currentMsg && playTime >= currentMsg->time * 1000LL; currentMsg = currentMsg->next) {
                switch (currentMsg->type) {
                    case TML_PROGRAM_CHANGE:
                        //channel program (preset) change (special handling for 10th MIDI channel with drums)
                        tsf_channel_set_presetnumber(synth, currentMsg->channel,
                                                     currentMsg->program,
                                                     (currentMsg->channel == 9));
                        break;
                    case TML_NOTE_ON: //play a note
                        tsf_channel_note_on(synth, currentMsg->channel, currentMsg->key,
                                            static_cast<float>(currentMsg->velocity) / 127.0f);
                        break;
                    case TML_NOTE_OFF: //stop a note
                        tsf_channel_note_off(synth, currentMsg->channel, currentMsg->key);
                        break;
                    case TML_PITCH_BEND: //pitch wheel modification
                        tsf_channel_set_pitchwheel(synth, currentMsg->channel,
                                                   currentMsg->pitch_bend);
                        break;
                    case TML_CONTROL_CHANGE: //MIDI controller messages
                        tsf_channel_midi_control(synth, currentMsg->channel, currentMsg->control,
                                                 currentMsg->control_value);
                        break;
                }
            }
        }

        oboe::DataCallbackResult
        Player::onAudioReady(oboe::AudioStream *audioStream, void *audioData, int32_t numFrames) {
            memset(audioData, 0, sizeof(float) * NUM_CHANNELS * numFrames);
            if (currentMsg == nullptr) {
                playerListener->postEvent(END_OF_MEDIA, playTime);
                currentMsg = media;
                playTime = 0;
                tsf_reset(synth);
                //Initialize preset on special 10th MIDI channel to use percussion sound bank (128) if available
                tsf_channel_set_bank_preset(synth, 9, 128, 0);
                if (looping == -1 || (--loopCount) > 0) {
                    playerListener->postEvent(START, playTime);
                } else {
                    return oboe::DataCallbackResult::Stop;
                }
            }
            if (timeToSet != -1) {
                if (timeToSet < playTime) {
                    tsf_reset(synth);
                    //Initialize preset on special 10th MIDI channel to use percussion sound bank (128) if available
                    tsf_channel_set_bank_preset(synth, 9, 128, 0);
                    currentMsg = media;
                }
                playTime = timeToSet;
                timeToSet = -1;
                processEvents();
            }
            //Number of samples to process
            int sampleBlock = TSF_RENDER_EFFECTSAMPLEBLOCK;
            auto *stream = static_cast<float *>(audioData);
            for (; numFrames > 0; numFrames -= sampleBlock, stream += sampleBlock * NUM_CHANNELS) {
                //We progress the MIDI playback and then process TSF_RENDER_EFFECTSAMPLEBLOCK samples at once
                if (sampleBlock > numFrames) sampleBlock = numFrames;

                playTime += sampleBlock * 1000000LL / audioStream->getSampleRate();
                processEvents();

                // Render the block of audio samples in float format
                tsf_render_float(synth, stream, sampleBlock);
            }
            return oboe::DataCallbackResult::Continue;
        }
    } // namespace tiny
} // namespace mmapi
