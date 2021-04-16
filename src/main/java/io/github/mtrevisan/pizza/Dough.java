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
import io.github.mtrevisan.pizza.yeasts.YeastModelAbstract;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BracketingNthOrderBrentSolver;
import org.apache.commons.math3.exception.NoBracketingException;
import org.apache.commons.math3.exception.TooManyEvaluationsException;


public class Dough{

	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_CARBON = 12.0107;
	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_HYDROGEN = 1.00784;
	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_OXYGEN = 15.9994;
	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_SODIUM = 22.989769;
	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_CHLORINE = 35.453;

	/** Molecular weight of glucose [g/mol]. */
	private static final double MOLECULAR_WEIGHT_GLUCOSE = MOLECULAR_WEIGHT_CARBON * 6. + MOLECULAR_WEIGHT_HYDROGEN * 12.
		+ MOLECULAR_WEIGHT_OXYGEN * 6.;
	/** [g/mol] */
	private static final double MOLECULAR_WEIGHT_SODIUM_CHLORIDE = MOLECULAR_WEIGHT_SODIUM + MOLECULAR_WEIGHT_CHLORINE;

	/** Standard atmosphere [hPa]. */
	static final double ONE_ATMOSPHERE = 1013.25;


	/**
	 * @see #sugarFactor()
	 */
	private static final double[] SUGAR_COEFFICIENTS = new double[]{1., 4.9, -50.};
	/**
	 * [%]
	 *
	 * @see #sugarFactor()
	 * @see #SUGAR_COEFFICIENTS
	 * @see <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6333755/">Stratford, Steels, Novodvorska, Archer, Avery. Extreme Osmotolerance and Halotolerance in Food-Relevant Yeasts and the Role of Glycerol-Dependent Cell Individuality. 2018.</a>
	 */
	static final double SUGAR_MAX = 3.21 * MOLECULAR_WEIGHT_GLUCOSE / 10.;

	/**
	 * TODO
	 * [%]
	 *
	 * @see #fatFactor()
	 */
	static final double FAT_MAX = 1.;

	/**
	 * (should be 2.04 * mol/l = 2.04 * MOLECULAR_WEIGHT_SODIUM_CHLORIDE / 10. % = 11.922324876 (?)) [%]
	 *
	 * @see #saltFactor()
	 * @see <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6333755/">Stratford, Steels, Novodvorska, Archer, Avery. Extreme Osmotolerance and Halotolerance in Food-Relevant Yeasts and the Role of Glycerol-Dependent Cell Individuality. 2018.</a>
	 */
	static final double SALT_MAX = 45.;

	/**
	 * @see #waterFactor()
	 * @see #HYDRATION_MIN
	 * @see #HYDRATION_MAX
	 */
	private static final double[] WATER_COEFFICIENTS = new double[]{-1.292, 7.65, -6.25};
	/**
	 * [%]
	 *
	 * @see #WATER_COEFFICIENTS
	 * @see #waterFactor()
	 */
	static final double HYDRATION_MIN = (7.65 - Math.sqrt(Math.pow(7.65, 2.) - 4. * 6.25 * 1.292)) / (2. * 6.25);
	/**
	 * [%]
	 *
	 * @see #WATER_COEFFICIENTS
	 * @see #waterFactor()
	 */
	static final double HYDRATION_MAX = (7.65 + Math.sqrt(Math.pow(7.65, 2.) - 4. * 6.25 * 1.292)) / (2. * 6.25);

	/**
	 * [mg/l] = 1 ppm
	 *
	 * @see #waterChlorineDioxideFactor()
	 */
	public static final double WATER_CHLORINE_DIOXIDE_MAX = 1. / 0.0931;
	/**
	 * TODO
	 * [mg/l]
	 *
	 * @see #waterFixedResidueFactor()
	 */
	public static final double WATER_FIXED_RESIDUE_MAX = 1500.;

	/**
	 * @see #atmosphericPressureFactor()
	 * @see #ATMOSPHERIC_PRESSURE_MAX
	 */
	private static final double PRESSURE_FACTOR_K = 1.46;
	/**
	 * @see #atmosphericPressureFactor()
	 * @see #ATMOSPHERIC_PRESSURE_MAX
	 */
	private static final double PRESSURE_FACTOR_M = 2.031;
	/**
	 * Minimum inhibitory pressure [hPa].
	 *
	 * @see #atmosphericPressureFactor()
	 * @see #PRESSURE_FACTOR_K
	 * @see #PRESSURE_FACTOR_M
	 */
	public static final double ATMOSPHERIC_PRESSURE_MAX = Math.pow(10_000., 2.) * Math.pow(1. / PRESSURE_FACTOR_K, (1. / PRESSURE_FACTOR_M));

	/**
	 * [%]
	 *
	 * @see #calculateYeast(Procedure)
	 */
	private static final double SOLVER_YEAST_MAX = 1.;
	private static final int SOLVER_EVALUATIONS_MAX = 100;

	//densities: http://www.fao.org/3/a-ap815e.pdf
	//plot graphs: http://www.shodor.org/interactivate/activities/SimplePlot/
	//regression: https://planetcalc.com/5992/
	//regression: https://planetcalc.com/8735/
	//regression: http://www.colby.edu/chemistry/PChem/scripts/lsfitpl.html


	//accuracy is ±0.001%
	private final BracketingNthOrderBrentSolver solverYeast = new BracketingNthOrderBrentSolver(0.000_01, 5);


	private final YeastModelAbstract yeastModel;
	/** Total sugar (glucose) quantity w.r.t. flour [%]. */
	private double sugar;
	/** Total fat quantity w.r.t. flour [%]. */
	private double fat;
	/** Total salt quantity w.r.t. flour [%]. */
	private double salt;
	/** Total water quantity w.r.t. flour [%]. */
	private double water;
	/** Chlorine dioxide in water [mg/l]. */
	private double waterChlorineDioxide;
	/** Fixed residue in water [mg/l]. */
	private double waterFixedResidue;
	/** Atmospheric pressure [hPa]. */
	private double atmosphericPressure = ONE_ATMOSPHERE;

	/** Yeast quantity [%]. */
	double yeast;


	//TODO time[hrs] = 0.0665 * Math.pow(FY[%], -0.7327) (@ 25 °C), inverse is FY[%] = Math.pow(time[hrs] / 0.0665, 1. / -0.7327)
	//https://www.pizzamaking.com/forum/index.php?topic=22649.20
	//https://www.pizzamaking.com/forum/index.php?topic=26831.0
	//https://www.pizzamaking.com/forum/index.php?topic=22649.msg229864#msg229864


	public static Dough create(final YeastModelAbstract yeastModel) throws DoughException{
		return new Dough(yeastModel);
	}

	private Dough(final YeastModelAbstract yeastModel) throws DoughException{
		if(yeastModel == null)
			throw DoughException.create("A yeast model must be provided");

		this.yeastModel = yeastModel;
	}


	/**
	 * @param sugar	Sugar quantity w.r.t. flour [%].
	 * @param ingredients	The recipe ingredients.
	 * @return	This instance.
	 * @throws DoughException	If sugar is too low or too high.
	 */
	public Dough addSugar(final double sugar, final Ingredients ingredients) throws DoughException{
		return addSugar(sugar, ingredients.sugarType, ingredients.sugarContent, ingredients.sugarWaterContent);
	}

	/**
	 * @param sugar	Sugar quantity w.r.t. flour [%].
	 * @param sugarType	Sugar type.
	 * @param sugarContent	Sucrose content [%].
	 * @param waterContent	Water content [%].
	 * @return	This instance.
	 * @throws DoughException	If sugar is too low or too high.
	 */
	public Dough addSugar(final double sugar, final SugarType sugarType, final double sugarContent, final double waterContent)
			throws DoughException{
		if(sugar < 0.)
			throw DoughException.create("Sugar [%] must be positive");

		this.sugar += sugarType.factor * sugar * sugarContent;
		addPureWater(sugar * waterContent);

		return this;
	}

	/**
	 * @param fat	Fat quantity w.r.t. flour [%].
	 * @param ingredients	The recipe ingredients.
	 * @return	This instance.
	 * @throws DoughException	If fat is too low or too high.
	 */
	public Dough addFat(final double fat, final Ingredients ingredients) throws DoughException{
		return addFat(fat, ingredients.fatContent, ingredients.fatWaterContent, ingredients.fatSaltContent);
	}

	/**
	 * @param fat	Fat quantity w.r.t. flour [%].
	 * @param fatContent	Sucrose content [%].
	 * @param waterContent	Water content [%].
	 * @param saltContent	Salt content [%].
	 * @return	This instance.
	 * @throws DoughException	If fat is too low or too high.
	 */
	public Dough addFat(final double fat, final double fatContent, final double waterContent, final double saltContent)
			throws DoughException{
		this.fat += fat * fatContent;
		addPureWater(fat * waterContent);
		addSalt(fat * saltContent);

		if(fat < 0. || this.fat > FAT_MAX)
			throw DoughException.create("Fat [%] must be between 0 and " + Helper.round(FAT_MAX * 100., 1) + "%");

		return this;
	}

	/**
	 * @param salt	Salt quantity w.r.t. flour [%].
	 * @return	This instance.
	 * @throws DoughException	If salt is too low or too high.
	 */
	public Dough addSalt(final double salt) throws DoughException{
		if(salt < 0.)
			throw DoughException.create("Salt [%] must be positive");

		this.salt += salt;

		return this;
	}

	/**
	 * @param water	Water quantity w.r.t. flour [%].
	 * @return	This instance.
	 * @throws DoughException	If water is too low.
	 */
	public Dough addPureWater(final double water) throws DoughException{
		return addWater(water, 0., 0.);
	}

	/**
	 * @param water	Water quantity w.r.t. flour [%].
	 * @param ingredients	The recipe ingredients.
	 * @return	This instance.
	 * @throws DoughException	If water is too low, or chlorine dioxide is too low or too high, or fixed residue is too low or too high.
	 */
	public Dough addWater(final double water, final Ingredients ingredients) throws DoughException{
		return addWater(water, ingredients.waterChlorineDioxide, ingredients.waterFixedResidue);
	}

	/**
	 * @param water	Water quantity w.r.t. flour [%].
	 * @param chlorineDioxide	Chlorine dioxide in water [mg/l].
	 * @param fixedResidue	Fixed residue in water [mg/l].
	 * @return	This instance.
	 * @throws DoughException	If water is too low, or chlorine dioxide is too low or too high, or fixed residue is too low or too high.
	 */
	public Dough addWater(final double water, final double chlorineDioxide, final double fixedResidue) throws DoughException{
		if(water < 0.)
			throw DoughException.create("Hydration [%] cannot be less than zero");
		if(chlorineDioxide < 0. || chlorineDioxide >= WATER_CHLORINE_DIOXIDE_MAX)
			throw DoughException.create("Chlorine dioxide [mg/l] in water must be between 0 and "
				+ Helper.round(WATER_CHLORINE_DIOXIDE_MAX, 2) + " mg/l");
		if(fixedResidue < 0. || fixedResidue >= WATER_FIXED_RESIDUE_MAX)
			throw DoughException.create("Fixed residue [mg/l] of water must be between 0 and "
				+ Helper.round(WATER_FIXED_RESIDUE_MAX, 2) + " mg/l");

		waterChlorineDioxide = (this.water * waterChlorineDioxide + water * chlorineDioxide) / (this.water + water);
		waterFixedResidue = (this.water * waterFixedResidue + water * fixedResidue) / (this.water + water);
		this.water += water;

		return this;
	}

	public Dough withAtmosphericPressure(final double atmosphericPressure) throws DoughException{
		if(atmosphericPressure < 0. || atmosphericPressure >= ATMOSPHERIC_PRESSURE_MAX)
			throw DoughException.create("Atmospheric pressure [hPa] must be between 0 and "
				+ Helper.round(ATMOSPHERIC_PRESSURE_MAX, 1) + " hPa");

		this.atmosphericPressure = atmosphericPressure;

		return this;
	}

	/**
	 * @see <a href="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6333755/">Stratford, Steels, Novodvorska, Archer, Avery. Extreme Osmotolerance and Halotolerance in Food-Relevant Yeasts and the Role of Glycerol-Dependent Cell Individuality. 2018.</a>
	 *
	 * @throws DoughException	If validation fails.
	 */
	private void validate() throws DoughException{
		if(water < HYDRATION_MIN || water > HYDRATION_MAX)
			throw DoughException.create("Hydration [%] must be between " + Helper.round(HYDRATION_MIN * 100., 1)
				+ "% and " + Helper.round(HYDRATION_MAX * 100., 1) + "%");
		if(fractionOverTotal(sugar) > SUGAR_MAX)
			throw DoughException.create("Sugar [%] must be less than " + Helper.round(SUGAR_MAX * 100., 1) + "%");
		if(fractionOverTotal(salt) > SALT_MAX)
			throw DoughException.create("Salt [%] must be less than " + Helper.round(SALT_MAX * 100., 1) + "%");

		//convert [%] to [mol/l]
		final double glucose = fractionOverTotal(sugar * 10.) / MOLECULAR_WEIGHT_GLUCOSE;
		//convert [%] to [mol/l]
		final double sodiumChloride = fractionOverTotal(salt * 10.) / MOLECULAR_WEIGHT_SODIUM_CHLORIDE;
		if(glucose <= 0.3 && sodiumChloride > Math.exp((1. - Math.log(Math.pow(1. + Math.exp(1.0497 * glucose), 1.3221)))
				* (glucose / (0.0066 + 0.7096 * glucose))) || glucose > 0.3 && sodiumChloride > 1.9930 * (3. - glucose) / 2.7)
			throw DoughException.create("Salt and sugar are too much, yeast will die");
	}


	/**
	 * Find the initial yeast able to obtain a given volume expansion ratio after a series of consecutive stages at a given duration at
	 * temperature.
	 *
	 * @param procedure	Data for procedure.
	 */
	void calculateYeast(final Procedure procedure) throws DoughException, YeastException{
		validate();

		try{
			final double ingredientsFactor = ingredientsFactor();
			final UnivariateFunction f = yeast -> {
				final double alpha = maximumRelativeVolumeExpansionRatio(yeast);
				double lambda = estimatedLag(yeast);
				LeaveningStage currentStage = procedure.leaveningStages[0];
				double volumeExpansionRatio = 0.;
				double duration = 0.;
				if(procedure.targetVolumeExpansionRatioAtLeaveningStage > 0){
					int stretchAndFoldIndex = 0;
					double stretchAndFoldDuration = 0.;
					for(int i = 1; i < procedure.leaveningStages.length; i ++){
						final LeaveningStage previousStage = procedure.leaveningStages[i - 1];
						duration += previousStage.duration;
						currentStage = procedure.leaveningStages[i];

						//avoid modifying `lambda` if the temperature is the same
						double currentVolume = 0.;
						if(previousStage.temperature != currentStage.temperature){
							final double previousVolume = yeastModel.volumeExpansionRatio(duration, lambda, alpha, previousStage.temperature,
								ingredientsFactor);
							lambda = Math.max(lambda - previousStage.duration, 0.);
							currentVolume = yeastModel.volumeExpansionRatio(duration, lambda, alpha, currentStage.temperature, ingredientsFactor);

							volumeExpansionRatio += previousVolume - currentVolume;
						}
						//account for stage volume decrease
						volumeExpansionRatio -= currentVolume * previousStage.volumeDecrease;

						//apply stretch&fold volume reduction:
						double stretchAndFoldVolumeDecrease = 0.;
						while(procedure.stretchAndFoldStages != null && stretchAndFoldIndex < procedure.stretchAndFoldStages.length){
							final StretchAndFoldStage stretchAndFoldStage = procedure.stretchAndFoldStages[stretchAndFoldIndex];
							if(stretchAndFoldDuration + stretchAndFoldStage.lapse > duration)
								break;

							stretchAndFoldIndex ++;
							stretchAndFoldDuration += stretchAndFoldStage.lapse;

							final double volumeAtStretchAndFold = yeastModel.volumeExpansionRatio(duration - previousStage.duration
								+ stretchAndFoldDuration, lambda, alpha, currentStage.temperature, ingredientsFactor);
							stretchAndFoldVolumeDecrease += (volumeAtStretchAndFold - stretchAndFoldVolumeDecrease)
								* stretchAndFoldStage.volumeDecrease;
						}
						volumeExpansionRatio -= stretchAndFoldVolumeDecrease;

						//early exit if target volume expansion ratio references an inner stage
						if(i == procedure.targetVolumeExpansionRatioAtLeaveningStage)
							break;
					}
				}

				//NOTE: last `stage.volumeDecrease` is NOT taken into consideration!
				volumeExpansionRatio += yeastModel.volumeExpansionRatio(duration + currentStage.duration, lambda, alpha,
					currentStage.temperature, ingredientsFactor);
				return volumeExpansionRatio * (1. - currentStage.volumeDecrease) - procedure.targetVolumeExpansionRatio;
			};
			yeast = solverYeast.solve(SOLVER_EVALUATIONS_MAX, f, 0., SOLVER_YEAST_MAX);
		}
		catch(final NoBracketingException e){
			throw YeastException.create("No yeast quantity will ever be able to produce the given expansion ratio");
		}
		catch(final TooManyEvaluationsException e){
			throw YeastException.create("Cannot calculate yeast quantity, try increasing maximum number of evaluations in the solver");
		}
	}

	/**
	 * Maximum relative volume expansion ratio.
	 *
	 * @see <a href="https://mohagheghsho.ir/wp-content/uploads/2020/01/Description-of-leavening-of-bread.pdf">Description of leavening of bread dough with mathematical modelling</a>
	 *
	 * @param yeast	Quantity of yeast [%].
	 * @return	The estimated lag [hrs].
	 */
	double maximumRelativeVolumeExpansionRatio(final double yeast){
		//FIXME this formula is for 36±1 °C
		//vertex must be at 1.1%
		return (yeast < 0.011? 24_546. * (0.022 - yeast) * yeast: 2.97);
	}

	/**
	 * @see <a href="https://mohagheghsho.ir/wp-content/uploads/2020/01/Description-of-leavening-of-bread.pdf">Description of leavening of bread dough with mathematical modelling</a>
	 * @see <a href="https://meridian.allenpress.com/jfp/article/71/7/1412/172677/Individual-Effects-of-Sodium-Potassium-Calcium-and">Bautista-Gallego, Arroyo-López, Durán-Quintana, Garrido-Fernández. Individual Effects of Sodium, Potassium, Calcium, and Magnesium Chloride Salts on Lactobacillus pentosus and Saccharomyces cerevisiae Growth. 2008.</a>
	 *
	 * @param yeast	Quantity of yeast [%].
	 * @return	The estimated lag [hrs].
	 */
	public double estimatedLag(final double yeast){
		//transform [%] to [g/l]
		final double s = fractionOverTotal(salt * 10.);
		final double saltLag = Math.log(1. + Math.exp(0.494 * (s - 84.)));

		//FIXME this formula is for 36±1 °C
		final double lag = (yeast > 0.? 0.0068 * Math.pow(yeast, -0.937): Double.POSITIVE_INFINITY);

		return lag + saltLag;
	}

	/**
	 * Modify specific growth ratio in order to account for sugar, fat, salt, water, and chlorine dioxide.
	 * <p>
	 * Yeast activity is impacted by:
	 * <ul>
	 *    <li>quantity percent of flour</li>
	 *    <li>temperature</li>
	 *    <li>hydration</li>
	 *    <li>salt</li>
	 *    <li>fat (*)</li>
	 *    <li>sugar</li>
	 *    <li>yeast age (*)</li>
	 *    <li>dough ball size (*)</li>
	 *    <li>gluten development (*)</li>
	 *    <li>altitude (atmospheric pressure)</li>
	 *    <li>water chemistry (level of chlorination especially)</li>
	 *    <li>container material and thickness (conductivity if ambient and dough temperatures vary, along with heat dissipation from fermentation) (*)</li>
	 *    <li>flour chemistry (enzyme activity, damaged starch, etc.) (*)</li>
	 * </ul>
	 * </p>
	 *
	 * @return	Factor to be applied to maximum specific growth rate.
	 */
	private double ingredientsFactor(){
		final double kSugar = sugarFactor();
		final double kFat = fatFactor();
		final double kSalt = saltFactor();
		final double kWater = waterFactor();
		final double kWaterChlorineDioxide = waterChlorineDioxideFactor();
		final double kWaterFixedResidue = waterFixedResidueFactor();
		final double kHydration = kWater * kWaterChlorineDioxide * kWaterFixedResidue;
		final double kAtmosphericPressure = atmosphericPressureFactor();
		return kSugar * kFat * kSalt * kHydration * kAtmosphericPressure;
	}

	/**
	 * @see <a href="https://uwaterloo.ca/chem13-news-magazine/april-2015/activities/fermentation-sugars-using-yeast-discovery-experiment">The fermentation of sugars using yeast: A discovery experiment</a>
	 * @see <a href="https://www.bib.irb.hr/389483/download/389483.Arroyo-Lopez_et_al.pdf">Arroyo-López, Orlic, Querol, Barrio. Effects of temperature, pH and sugar concentration on the growth parameters of Saccharomyces cerevisiae, S. kudriavzevii and their interspecific hybrid. 2009.</a>
	 * @see <a href="http://www.biologydiscussion.com/industrial-microbiology-2/yeast-used-in-bakery-foods/yeast-used-in-bakery-foods-performance-determination-forms-effect-industrial-microbiology/86555">Yeast used in bakery foods: Performance, determination, forms & effect. Industrial Microbiology</a>
	 *
	 * @return	Correction factor.
	 */
	double sugarFactor(){
		if(sugar < 0.03)
			return Math.min(Helper.evaluatePolynomial(SUGAR_COEFFICIENTS, fractionOverTotal(sugar)), 1.);
		if(sugar < SUGAR_MAX)
			return Math.max(-0.3545 * (Math.log(fractionOverTotal(sugar)) - Math.log(SUGAR_MAX)), 0.);
		return 0.;
	}

	/**
	 * TODO high fat content inhibits leavening
	 * 0.1-0.2% is desirable
	 *
	 * @return	Correction factor.
	 */
	double fatFactor(){
		//0 <= fat <= FAT_MAX
		//1+fat/300?
		return 1.;
	}

	/**
	 * @see <a href="https://www.microbiologyresearch.org/docserver/fulltext/micro/64/1/mic-64-1-91.pdf">Watson. Effects of Sodium Chloride on Steady-state Growth and Metabolism of Saccharomyces cerevisiae. 1970. Journal of General Microbiology. Vol 64.</a>
	 * @see <a href="https://aem.asm.org/content/aem/43/4/757.full.pdf">Wei, Tanner, Malaney. Effect of Sodium Chloride on baker's yeast growing in gelatin. 1981. Applied and Environmental Microbiology. Vol. 43, No. 4.</a>
	 * @see <a href="https://meridian.allenpress.com/jfp/article/70/2/456/170132/Use-of-Logistic-Regression-with-Dummy-Variables">López, Quintana, Fernández. Use of logistic regression with dummy variables for modeling the growth–no growth limits of Saccharomyces cerevisiae IGAL01 as a function of Sodium chloride, acid type, and Potassium Sorbate concentration according to growth media. 2006. Journal of Food Protection. Vol 70, No. 2.</a>
	 * @see <a href="https://undergradsciencejournals.okstate.edu/index.php/jibi/article/view/2512">Lenaburg, Kimmons, Kafer, Holbrook, Franks. Yeast Growth: The effect of tap water and distilled water on yeast fermentation with salt additives. 2016.</a>
	 * @see <a href="https://meridian.allenpress.com/jfp/article/71/7/1412/172677/Individual-Effects-of-Sodium-Potassium-Calcium-and">Bautista-Gallego, Arroyo-López, Durán-Quintana, Garrido-Fernández. Individual Effects of Sodium, Potassium, Calcium, and Magnesium Chloride Salts on Lactobacillus pentosus and Saccharomyces cerevisiae Growth. 2008.</a>
	 * @see <a href="https://onlinelibrary.wiley.com/doi/abs/10.1002/jsfa.4575">Beck, Jekle, Becker. Impact of sodium chloride on wheat flour dough for yeast-leavened products. II. Baking quality parameters and their relationship. 2010.</a>
	 *
	 * @return	Correction factor.
	 */
	double saltFactor(){
		final double x = 11.7362 * salt;
		final double a = (Double.isInfinite(Math.exp(x))? 1. - 0.0256 * x: 1. - Math.log(Math.pow(1. + Math.exp(x), 0.0256)));
		final double b = salt / (87.5679 - 0.2725 * salt);
		return Math.exp(a * b);
	}

	/**
	 * https://buonapizza.forumfree.it/?t=75686746
	 * @see <a href="https://www.nature.com/articles/s41598-018-36786-2.pdf">Minervini, Dinardo, de Angelis, Gobbetti. Tap water is one of the drivers that establish and assembly the lactic acid bacterium biota during sourdough preparation. 2018.</a>
	 * @see <a href="http://fens.usv.ro/index.php/FENS/article/download/328/326">Codina, Mironeasa, Voica. Influence of wheat flour dough hydration levels on gas production during dough fermentation and bread quality. 2011. Journal of Faculty of Food Engineering. Vol. X, Issue 4.</a>
	 *
	 * @return	Correction factor.
	 */
	double waterFactor(){
		return (HYDRATION_MIN <= water && water < HYDRATION_MAX? Helper.evaluatePolynomial(WATER_COEFFICIENTS, water): 0.);
	}

	/**
	 * https://academic.oup.com/mutage/article/19/2/157/1076450
	 * Buschini, Carboni, Furlini, Poli, Rossi. sodium hypochlorite-, chlorine dioxide- and peracetic acid-induced genotoxicity detected by Saccharomyces cerevisiae tests [2004]
	 *
	 * @return	Correction factor.
	 */
	double waterChlorineDioxideFactor(){
		return Math.max(1. - waterChlorineDioxide * fractionOverTotal(water) / WATER_CHLORINE_DIOXIDE_MAX, 0.);
	}

	/**
	 * TODO Se la durezza dell’acqua è troppo elevata la fermentazione subisce rallentamenti a causa della formazione di una struttura glutinica troppo rigida. In caso contrario, dove la durezza dell’acqua risulta essere troppo scarsa, l’impasto si presenta assai appiccicoso e poco manipolabile. In questo frangente sarà utile abbassare l’idratazione.
	 *
	 * @return	Correction factor.
	 */
	double waterFixedResidueFactor(){
		//0 <= fixedResidue <= WATER_FIXED_RESIDUE_MAX
		return 1.;
	}

	/**
	 * @see <a href="https://www.tandfonline.com/doi/pdf/10.1271/bbb.69.1365">Arao, Hara, Suzuki, Tamura. Effect of High-Pressure Gas on io.github.mtrevisan.pizza.Yeast Growth. 2014.</a>
	 *
	 * @return	Correction factor.
	 */
	double atmosphericPressureFactor(){
		return (atmosphericPressure < ATMOSPHERIC_PRESSURE_MAX?
			1. - PRESSURE_FACTOR_K * Math.pow(atmosphericPressure / Math.pow(10_000., 2.), PRESSURE_FACTOR_M): 0.);
	}

	private double fractionOverTotal(final double value){
		return value / (1. + water);
	}


	/**
	 * @param ingredients	The recipe ingredients.
	 * @return	The recipe.
	 */
	public Recipe createRecipe(final Ingredients ingredients, final Procedure procedure) throws DoughException, YeastException{
		calculateYeast(procedure);

		final double totalFraction = 1. + water + sugar + yeast + salt + fat;
		double totalFlour = ingredients.dough / totalFraction;
		double yeast, flour, water, sugar, fat, salt,
			difference;
		final double waterCorrection = (ingredients.correctForIngredients?
			this.sugar * ingredients.sugarWaterContent + this.fat * ingredients.fatWaterContent: 0.)
			+ (ingredients.correctForHumidity? Flour.estimatedHumidity(ingredients.airRelativeHumidity)
			- Flour.estimatedHumidity(0.706): 0.);
		do{
			yeast = totalFlour * this.yeast / (ingredients.yeastType.factor * ingredients.rawYeast);
			flour = totalFlour - yeast * (1. - ingredients.rawYeast);
			water = Math.max(totalFlour * this.water - waterCorrection, 0.);
			sugar = totalFlour * this.sugar / (ingredients.sugarType.factor * ingredients.sugarContent);
			final double fatCorrection = (ingredients.correctForIngredients? flour * ingredients.flour.fatContent: 0.);
			fat = Math.max(totalFlour * this.fat - fatCorrection, 0.) / ingredients.fatContent;
			final double saltCorrection = (ingredients.correctForIngredients? flour * ingredients.flour.saltContent
				+ this.fat * ingredients.fatSaltContent: 0.);
			salt = Math.max(totalFlour * this.salt - saltCorrection, 0.);

			//refine approximation:
			final double calculatedDough = flour + water + yeast + sugar + salt + fat;
			difference = ingredients.dough - calculatedDough;
			totalFlour += difference * 0.6;
		}while(Math.abs(difference) > ingredients.doughPrecision);
		final double waterTemperature = (ingredients.dough * ingredients.doughTemperature
			- (ingredients.dough - water) * ingredients.ingredientsTemperature) / water;

		final Recipe recipe = new Recipe();
		recipe.flour = flour;
		recipe.water = water;
		recipe.waterTemperature = waterTemperature;
		recipe.yeast = yeast;
		recipe.sugar = sugar;
		recipe.fat = fat;
		recipe.salt = salt;
		return recipe;
	}


	/**
	 * @see <a href="https://www.academia.edu/2421508/Characterisation_of_bread_doughs_with_different_densities_salt_contents_and_water_levels_using_microwave_power_transmission_measurements">Campbell. Characterisation of bread doughs with different densities, salt contents and water levels using microwave power transmission measurements. 2005.</a>
	 * @see <a href="https://core.ac.uk/download/pdf/197306213.pdf">Kubota, Matsumoto, Kurisu, Sizuki, Hosaka. The equations regarding temperature and concentration of the density and viscosity of sugar, salt and skim milk solutions. 1980.</a>
	 * @see <a href="https://shodhganga.inflibnet.ac.in/bitstream/10603/149607/15/10_chapter%204.pdf">Density studies of sugar solutions</a>	 *
	 * @see <a href="https://www.researchgate.net/publication/280063894_Mathematical_modelling_of_density_and_viscosity_of_NaCl_aqueous_solutions">Simion, Grigoras, Rosu, Gavrila. Mathematical modelling of density and viscosity of NaCl aqueous solutions. 2014.</a>
	 * @see <a href="https://www.researchgate.net/publication/233266779_Temperature_and_Concentration_Dependence_of_Density_of_Model_Liquid_Foods">Darros-Barbosa, Balaban, Teixeira.Temperature and concentration dependence of density of model liquid foods. 2003.</a>
	 *
	 * @param flour	Flour weight [g].
	 * @param dough	Final dough weight [g].
	 * @param fatDensity	Density of the fat [kg/l].
	 * @param doughTemperature	Temperature of the dough [°C].
	 */
	public double doughVolume(final double flour, final double dough, final double fatDensity, final double doughTemperature){
		//TODO
		//density of flour + salt + sugar + water
		double doughDensity = 1.41
			- 0.00006762 * atmosphericPressure
			+ 0.00640 * salt
//			+ 0.00746 * salt - 0.000411 * (doughTemperature + ABSOLUTE_ZERO)
//			+ 0.000426 * sugar - 0.000349 * (doughTemperature + ABSOLUTE_ZERO)
			- 0.00260 * water;

		//account for fat
		final double fraction = fat * flour / dough;
		doughDensity = 1. / ((1. - fraction) / doughDensity + fraction / fatDensity);

		return dough / doughDensity;
	}

}
