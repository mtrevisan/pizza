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


public final class StretchAndFoldStage{

	//V_after = V_prior * (1 - VOLUME_DECREASE) [% v/v]
	public static final double VOLUME_DECREASE_DEFAULT = 0.4187;


	/** Interval at which to apply stretch & fold [hrs]. */
	final Duration lapse;
	/** Volume decrease after stretch & fold phase [% v/v]. */
	double volumeDecrease;


	/**
	 * @param lapse	Interval at which to apply stretch & fold [hrs].
	 * @return	The instance.
	 */
	public static StretchAndFoldStage create(final Duration lapse){
		return new StretchAndFoldStage(lapse);
	}

	private StretchAndFoldStage(final Duration lapse){
		this.lapse = lapse;
		volumeDecrease = VOLUME_DECREASE_DEFAULT;
	}

	/**
	 * @param volumeDecrease	Volume decrease after stretch & fold phase [% v/v].
	 * @return	The instance.
	 */
	public StretchAndFoldStage withVolumeDecrease(final double volumeDecrease) throws DoughException{
		if(volumeDecrease <= 0. || volumeDecrease >= 1.)
			throw DoughException.create("Volume decrease [% v/v] should be between 0 and 1");

		this.volumeDecrease = volumeDecrease;

		return this;
	}

	@Override
	public String toString(){
		return getClass().getSimpleName() + "{lapse: " + lapse + " hrs, reduction: "
			+ Helper.round(volumeDecrease * 100., 2) + "%}";
	}

}
