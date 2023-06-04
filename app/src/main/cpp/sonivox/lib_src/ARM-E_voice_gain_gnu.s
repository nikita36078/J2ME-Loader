@***********************************************************
@ Function:    WT_VoiceGain
@ Processor:   ARM-E
@ Description: the main synthesis function when fetching 
@			   wavetable samples.
@              C-callable.
@
@ Usage: 
@ Usage: 
@	WT_VoiceGain(
@			S_WT_VOICE *pWTVoice,
@			S_WT_FRAME *pWTFrame);
@
@ Copyright 2004, 2005 Sonic Network, Inc.
@****************************************************************
@ Revision Control:
@   $Revision: 814 $
@   $Date: 2007-08-02 10:34:53 -0700 (Thu, 02 Aug 2007) $
@****************************************************************
@
@   where:
@	S_WT_VOICE *psVoice
@	PASSED IN: r0
@
@	S_WT_FRAME *pWTFrame
@	PASSED IN: r1
@****************************************************************



	#include	"ARM_synth_constants_gnu.inc"

	.arm
	.text

	.global	WT_VoiceGain
	
@ Register usage
@ --------------
pWTVoice	.req	r0
pWTFrame	.req	r1
pInputBuffer	.req	r2
pMixBuffer	.req	r3

tmp0	.req	r4
tmp1	.req	r5
tmp2	.req	r1	@ reuse register
tmp3	.req	r6

numSamples	.req	r9

	#if	STEREO_OUTPUT
gainIncLeft	.req	r7
gainIncRight	.req	r8
gainLeft	.req	r10
gainRight	.req	r11
	#else
gainIncrement	.req	r7
gain	.req	r8
	#endif


@ register context for local variables
@SaveRegs	RLIST	{r4-r11,lr}
@RestoreRegs	RLIST	{r4-r11,pc}

WT_VoiceGain:

	STMFD	sp!, {r4-r11,lr}

	LDR		pInputBuffer, [pWTFrame, #m_pAudioBuffer]
	LDR		pMixBuffer, [pWTFrame, #m_pMixBuffer]
	LDR		numSamples, [pWTFrame, #m_numSamples]
	
@----------------------------------------------------------------
@ Stereo version
@----------------------------------------------------------------
@ NOTE: instructions are reordered to reduce the effect of latency 
@ due to storage and computational dependencies.
@----------------------------------------------------------------

	#if	STEREO_OUTPUT

	LDR		tmp0, [pWTFrame, #m_prevGain]
	LDR		tmp1, [pWTFrame, #m_gainTarget]
	
	LDRSH	gainLeft, [pWTVoice, #m_gainLeft]
	LDRSH	gainRight, [pWTVoice, #m_gainRight]
	
	MOV		gainIncLeft, gainLeft
	SMULBB	gainLeft, tmp0, gainLeft

	SMULBB	gainIncLeft, tmp1, gainIncLeft
	SUB		gainIncLeft, gainIncLeft, gainLeft
	MOV		gainLeft, gainLeft, ASR #(NUM_MIXER_GUARD_BITS - 2)
	MOV		gainIncLeft, gainIncLeft, ASR #(SYNTH_UPDATE_PERIOD_IN_BITS + NUM_MIXER_GUARD_BITS - 2)

	MOV		gainIncRight, gainRight
	SMULBB	gainRight, tmp0, gainRight

	SMULBB	gainIncRight, tmp1, gainIncRight
	SUB		gainIncRight, gainIncRight, gainRight
	MOV		gainRight, gainRight, ASR #(NUM_MIXER_GUARD_BITS - 2)
	MOV		gainIncRight, gainIncRight, ASR #(SYNTH_UPDATE_PERIOD_IN_BITS + NUM_MIXER_GUARD_BITS - 2)

	LDRSH		tmp0, [pInputBuffer], #2
	
StereoGainLoop:
	LDR		tmp1, [pMixBuffer]

	ADD		gainLeft, gainLeft, gainIncLeft

	SMLAWB	tmp1, gainLeft, tmp0, tmp1

	LDR		tmp2, [pMixBuffer, #4]

	ADD		gainRight, gainRight, gainIncRight

	STR		tmp1, [pMixBuffer], #4

	SMLAWB	tmp2, gainRight, tmp0, tmp2

	SUBS	numSamples, numSamples, #1

	LDRSHGT	tmp0, [pInputBuffer], #2
	
	STR		tmp2, [pMixBuffer], #4

	BGT		StereoGainLoop

@----------------------------------------------------------------
@ Mono version
@----------------------------------------------------------------
	#else

	LDR		gain, [pWTFrame, #m_prevGain]
	MOV		gain, gain, LSL #(NUM_MIXER_GUARD_BITS + 4)
	LDR		gainIncrement, [pWTFrame, #m_gainTarget]
	MOV		gainIncrement, gainIncrement, LSL #(NUM_MIXER_GUARD_BITS + 4)
	SUB		gainIncrement, gainIncrement, gain
	MOV		gainIncrement, gainIncrement, ASR #SYNTH_UPDATE_PERIOD_IN_BITS
	
MonoGainLoop:

	LDRSH	tmp0, [pInputBuffer], #NEXT_OUTPUT_PCM	@ fetch voice output
	
	LDR		tmp1, [pMixBuffer]						@ get left channel output sample
	ADD		gain, gain, gainIncrement				@ gain step to eliminate zipper noise
	SMULWB	tmp0, gain, tmp0 						@ sample * local gain

	MOV		tmp0, tmp0, ASR #1						@ add 6dB headroom
	ADD 	tmp1, tmp0, tmp1
	STR		tmp1, [pMixBuffer], #4					@ save and bump pointer
	
	SUBS	numSamples, numSamples, #1
	BGT		MonoGainLoop

    #endif
@end Mono version

	LDMFD	sp!,{r4-r11,lr}
	BX		lr
	
	.end

