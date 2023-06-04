@***********************************************************
@ File:         ARM_synth_constants.inc
@ Processor:    ARM
@ Description:  Contains constants and defines, most of which
@               are mirrored in synth.h
@
@ Copyright Sonic Network Inc. 2004
@****************************************************************
@ Revision Control:
@   $Revision: 741 $
@   $Date: 2007-06-22 16:39:21 -0700 (Fri, 22 Jun 2007) $
@****************************************************************


    #ifdef  SAMPLE_RATE_8000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 5
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 32
    #endif

    #ifdef  SAMPLE_RATE_16000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 6
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 64
    #endif

    #ifdef  SAMPLE_RATE_20000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 7
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 128
    #endif

    #ifdef  SAMPLE_RATE_22050
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 7
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 128
    #endif

    #ifdef  SAMPLE_RATE_24000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 7
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 128
    #endif

    #ifdef  SAMPLE_RATE_32000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 7
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 128
    #endif

    #ifdef  SAMPLE_RATE_44100
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 8
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 256
    #endif

    #ifdef  SAMPLE_RATE_48000
    .equ    SYNTH_UPDATE_PERIOD_IN_BITS, 8
    .equ    BUFFER_SIZE_IN_MONO_SAMPLES, 256
    #endif


@ if the OUTPUT PCM sample is 16-bits, then when using indexed addressing,
@ the next sample is this many bytes away
    .equ    NEXT_OUTPUT_PCM, 2

@****************************************************************************
@/* macros for fractional phase accumulator */
    .equ    NUM_PHASE_FRAC_BITS, 15

    .equ    PHASE_FRAC_MASK, 0x7FFF

@ shift for phase accumulator when fraction carries over
    #ifdef  SAMPLES_8_BIT
    .equ    NEXT_INPUT_PCM_SHIFT, 0
    #endif

    #ifdef  SAMPLES_16_BIT
    .equ    NEXT_INPUT_PCM_SHIFT, 1
    #endif

@****************************************************************************
    .equ    NUM_MIXER_GUARD_BITS, 4

@****************************************************************************
@/* Envelope 1 (EG1) calculation macros */
    .equ    NUM_EG1_FRAC_BITS, 15

@****************************************************************************

    .equ    NUM_ENHANCER_FILTER_COEF_FRAC_BITS, 5

@****************************************************************************

@
@ I've temporarily given up on the idea of getting ADS/RV and gcc to
@ handle a struct in a compatible fashion. Switching to old fashion EQU
@

    #if FILTER_ENABLED
@**************************************
@ typedef struct s_filter_tag
    .equ    m_z1, 0
    .equ    m_z2, 2
    #endif

@**************************************
@ typedef struct s_wt_frame_tag
    .equ    m_gainTarget, 0
    .equ    m_phaseIncrement, 4

    #if FILTER_ENABLED
    .equ    m_k, 8
    .equ    m_b1, 12
    .equ    m_b2, 16
    .equ    m_pAudioBuffer, 20
    .equ    m_pMixBuffer, 24
    .equ    m_numSamples, 28
    .equ    m_prevGain, 32
    #else
    .equ    m_pAudioBuffer, 8
    .equ    m_pMixBuffer, 12
    .equ    m_numSamples, 16
    .equ    m_prevGain, 20
    #endif


@**************************************
@ typedef struct s_wt_voice_tag
    .equ    m_pLoopEnd, 0   @ /* points to last PCM sample (not 1 beyond last) */
    .equ    m_pLoopStart, 4 @ /* points to first sample at start of loop */
    .equ    m_pPhaseAccum, 8    @ /* points to first sample at start of loop */
    .equ    m_phaseFrac, 12 @ /* points to first sample at start of loop */

    #if STEREO_OUTPUT
    .equ    m_gainLeft, 16  @ /* current gain, left ch  */
    .equ    m_gainRight, 18 @ /* current gain, right ch */
    #endif


@****************************************************************************
@ enhancer
    .equ    m_nEnhancerFeedForward1, 0
    .equ    m_nEnhancerFeedback1, 1
    .equ    m_nDriveCoef, 2
    .equ    m_nEnhancerFeedback2, 3
    .equ    m_nWet, 4
    .equ    m_nDry, 5

    .equ    m_zF0L, 6   @ filter 1 zero state var, left
    .equ    m_zF1L, 8   @ filter 1 pole state var, left
    .equ    m_zF2L, 10  @ filter 2 zero state var, left
    .equ    m_zF0R, 12  @ filter 1 zero state var, right
    .equ    m_zF1R, 14  @ filter 1 pole state var, right
    .equ    m_zF2R, 16  @ filter 2 zero state var, right

@****************************************************************************



