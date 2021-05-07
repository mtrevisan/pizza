/**
 * Copyright (c) 2019-2020 Mauro Trevisan
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */
package io.github.mtrevisan.pizza;

import io.github.mtrevisan.pizza.utils.Helper;

import java.time.Duration;


public final class LeaveningStage{

	//V_after = V_prior * (1 - VOLUME_DECREASE_CUT) [% v/v]
	public static final double VOLUME_DECREASE_CUT_DEFAULT = 0.4187;

	public static final LeaveningStage ZERO = new LeaveningStage();


	/** Leavening temperature [°C]. */
	final double temperature;
	/** Leavening duration [hrs]. */
	final Duration duration;
	/** Duration of work done after stage [hrs]. */
	Duration afterStageWork;
	/** Volume decrease after leavening stage [% v/v]. */
	double volumeDecrease;


	/**
	 * @param temperature	Leavening temperature [°C].
	 * @param duration	Leavening duration [hrs].
	 * @return	The instance.
	 */
	public static LeaveningStage create(final double temperature, final Duration duration) throws DoughException{
		return new LeaveningStage(temperature, duration);
	}

	private LeaveningStage(){
		temperature = 0.;
		this.duration = Duration.ZERO;
		this.afterStageWork = Duration.ZERO;
	}

	private LeaveningStage(final double temperature, final Duration duration) throws DoughException{
		if(duration == null || duration.isNegative() || duration.isZero())
			throw DoughException.create("Duration should be present and posituve");

		this.temperature = temperature;
		this.duration = duration;
		this.afterStageWork = Duration.ZERO;
	}

	/**
	 * @param afterStageWork	Length of work done at the end of the stage [hrs].
	 * @return	The instance.
	 */
	public LeaveningStage withAfterStageWork(final Duration afterStageWork) throws DoughException{
		if(afterStageWork == null || afterStageWork.isNegative())
			throw DoughException.create("Work after stage should be present and non-negative");

		this.afterStageWork = afterStageWork;

		return this;
	}

	/**
	 * @param volumeDecrease	Volume decrease after leavening stage [% v/v].
	 * @return	The instance.
	 */
	public LeaveningStage withVolumeDecrease(final double volumeDecrease) throws DoughException{
		if(volumeDecrease <= 0. || volumeDecrease >= 1.)
			throw DoughException.create("Volume decrease [% v/v] should be between 0 and 1");

		this.volumeDecrease = volumeDecrease;

		return this;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName() + "{" + temperature + " °C for " + Helper.round(duration.toMinutes() / 60., 2)
			+ " hrs"
			+ (!afterStageWork.isZero()? ", after-stage work duration " + Helper.round(afterStageWork.toMinutes() / 60., 2)
			+ " hrs": "")
			+ (volumeDecrease > 0.? ", volume decrease " + Helper.round(volumeDecrease * 100., 1) + "%": "")
			+ "}";
	}

}
