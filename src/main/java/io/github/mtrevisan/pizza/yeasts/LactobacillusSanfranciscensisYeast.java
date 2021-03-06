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
 * Lactobacillus sanfranciscensis constants
 *
 * @see <a href="https://aem.asm.org/content/aem/77/7/2292.full.pdf">Temperature adaptation markedly determines evolution within the genus Saccharomyces</a>
 */
public class LactobacillusSanfranciscensisYeast extends YeastModelAbstract{

	@Override
	public double getTemperatureMin(){
		//± 1.5 °C
		return 4.5;
	}

	@Override
	double getTemperatureOpt(){
		//± 0.5 °C
		return 32.5;
	}

	@Override
	public double getTemperatureMax(){
		//± 0.1 °C
		return 41.0;
	}

	@Override
	double getMaximumSpecificGrowthRate(){
		return 0.71;
	}

}
