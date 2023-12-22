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
package io.github.mtrevisan.pizza.yeasts;


/**
 * Saccharomyces cerevisiae (strain PE35 M) constants
 * Masato fermentation (Peru)
 *
 * @see <a href="https://aem.asm.org/content/aem/77/7/2292.full.pdf">Temperature adaptation markedly determines evolution within the genus Saccharomyces. 2011.</a>
 */
public class SaccharomycesCerevisiaePE35MYeast extends YeastModelAbstract{

	@Override
	public final double getTemperatureMin(){
		//± 0.23 °C
		return 5.04;
	}

	@Override
	public final double getTemperatureOpt(){
		//± 0.22 °C
		return 29.99;
	}

	@Override
	public final double getTemperatureMax(){
		//± 0.12 °C
		return 45.48;
	}

	@Override
	public final double getMaximumSpecificVolumeGrowthRate(){
		//base is pH 5.4±0.1, 20 mg/l glucose
		//± 0.008 hrs^-1
		return 0.375;
	}

}