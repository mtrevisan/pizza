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


public final class Flour{

	/** W. */
	final double strength;
	/** Salt content [% w/w]. */
	final double saltContent;
	/** Fat content [% w/w]. */
	final double fatContent;


	public static Flour create(){
		return new Flour(0., 0., 0.);
	}

	/**
	 * @param strength	Salt content [% w/w].
	 * @return	The instance.
	 * @throws DoughException	If there are errors in the parameters' values.
	 */
	public static Flour create(final double strength) throws DoughException{
		return create(strength, 0., 0.);
	}

	/**
	 * @param strength	Strength.
	 * @param saltContent	Salt content [% w/w].
	 * @param fatContent	Fat content [% w/w].
	 * @return	The instance.
	 * @throws DoughException	If there are errors in the parameters' values.
	 */
	public static Flour create(final double strength, final double saltContent, final double fatContent) throws DoughException{
		if(strength <= 0.)
			throw DoughException.create("Strength mush be positive");
		if(saltContent < 0.)
			throw DoughException.create("Salt content must be non-negative");
		if(fatContent < 0.)
			throw DoughException.create("Fat content must be non-negative");

		return new Flour(strength, saltContent, fatContent);
	}

	private Flour(final double strength, final double saltContent, final double fatContent){
		this.strength = strength;
		this.saltContent = saltContent;
		this.fatContent = fatContent;
	}

	/**
	 * @see <a href="https://www.research.manchester.ac.uk/portal/files/54543624/FULL_TEXT.PDF">Trinh. Gas cells in bread dough. 2013.</a>
	 *
	 * @param hydration	[% w/w].
	 * @return	Protein content (standard error is 0.0466) [% w/w].
	 */
	public static double estimatedMinimumProteinContent(final double hydration){
		return (hydration - 0.320) / 2.15;
	}

	/**
	 * @param airRelativeHumidity	[% w/w].
	 * @return	Flour humidity [% w/w].
	 */
	public static double estimatedHumidity(final double airRelativeHumidity){
		//13.5% at RH 70.62%
		return 0.121 + 0.000_044 * Math.exp(8.16 * airRelativeHumidity);
	}

}
