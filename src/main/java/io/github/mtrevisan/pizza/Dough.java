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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Dough{

	private static final Logger LOGGER = LoggerFactory.getLogger(Dough.class);

	/** Standard atmosphere [hPa]. */
	public static final double ONE_ATMOSPHERE = 1013.25;

	/**
	 * @see #sugarFactor()
	 */
	private static final double[] SUGAR_COEFFICIENTS = new double[]{1., 4.9, -50.};
	/**
	 * [%]
	 *
	 * @see #sugarFactor()
	 * @see #SUGAR_COEFFICIENTS
	 * @see <a hreaf="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6333755/">Stratford, Steels, Novodvorska, Archer, Avery. Extreme Osmotolerance and Halotolerance in Food-Relevant Yeasts and the Role of Glycerol-Dependent Cell Individuality. 2018.</a>
	 * FIXME max is 3.21 mol/l
	 */
	public static final double SUGAR_MAX = Math.exp(-0.3154 / 0.403);

	/**
	 * TODO
	 * [%]
	 *
	 * @see #fatFactor()
	 */
	public static final double FAT_MAX = 1.;

	/**
	 * @see #saltFactor()
	 * @see #SALT_MAX
	 */
	private static final double[] SALT_COEFFICIENTS = new double[]{1., -0.05, -45., -1187.5};
	/**
	 * [%]
	 *
	 * @see #saltFactor()
	 * @see #SALT_COEFFICIENTS
	 * @see <a hreaf="https://www.ncbi.nlm.nih.gov/pmc/articles/PMC6333755/">Stratford, Steels, Novodvorska, Archer, Avery. Extreme Osmotolerance and Halotolerance in Food-Relevant Yeasts and the Role of Glycerol-Dependent Cell Individuality. 2018.</a>
	 * FIXME max is 2.04 mol/l
	 */
	public static final double SALT_MAX = 0.08321;

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
	public static final double HYDRATION_MIN = (7.65 - Math.sqrt(Math.pow(7.65, 2.) - 4. * 6.25 * 1.292)) / (2. * 6.25);
	/**
	 * [%]
	 *
	 * @see #WATER_COEFFICIENTS
	 * @see #waterFactor()
	 */
	public static final double HYDRATION_MAX = (7.65 + Math.sqrt(Math.pow(7.65, 2.) - 4. * 6.25 * 1.292)) / (2. * 6.25);

	/**
	 * [mg/l]
	 *
	 * @see #chlorineDioxideFactor()
	 */
	public static final double WATER_CHLORINE_DIOXIDE_MAX = 0.0931;
	/**
	 * TODO
	 * [mg/l]
	 *
	 * @see #fixedResidueFactor()
	 */
	public static final double WATER_FIXED_RESIDUE_MAX = 100.;

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
	 * @see #backtrackStages(LeaveningStage[], double, int, StretchAndFoldStage[])
	 */
	private static final double YEAST_MAX = 1.;
	private static final int SOLVER_EVALUATIONS_MAX = 100;

	//densities: http://www.fao.org/3/a-ap815e.pdf
	//plot graphs: http://www.shodor.org/interactivate/activities/SimplePlot/


	//accuracy is ±0.001%
	private final BracketingNthOrderBrentSolver solverYeast = new BracketingNthOrderBrentSolver(0.000_01, 5);


	private final YeastModelAbstract yeastModel;
	/** Sugar quantity w.r.t. flour [%]. */
	private double sugar;
	/** Fat quantity w.r.t. flour [%]. */
	private double fat;
	/** Salt quantity w.r.t. flour [%]. */
	private double salt;
	/** Water quantity w.r.t. flour [%]. */
	private double water;
	/** Chlorine dioxide in water [mg/l]. */
	private double waterChlorineDioxide;
	/** Fixed residue in water [mg/l]. */
	private double waterFixedResidue;
	/** Atmospheric pressure [hPa]. */
	private double atmosphericPressure = ONE_ATMOSPHERE;


	//https://planetcalc.com/5992/
	//TODO time[hrs] = 0.0665 * Math.pow(FY[%], -0.7327) (@ 25 °C), inverse is FY[%] = Math.pow(time[hrs] / 0.0665, 1. / -0.7327)
	//https://www.pizzamaking.com/forum/index.php?topic=22649.20
	//https://www.pizzamaking.com/forum/index.php?topic=26831.0


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
	 * @param sugarType	Sugar type.
	 * @param sugarContent	Sucrose content [%].
	 * @param waterContent	Water content [%].
	 * @return	This instance.
	 * @throws DoughException	If sugar is too low or too high.
	 */
	public Dough addSugar(final double sugar, final SugarType sugarType, final double sugarContent, final double waterContent)
			throws DoughException{
		this.sugar += sugarType.factor * sugar * sugarContent;
		addPureWater(sugar * waterContent);

		if(sugar < 0. || this.sugar > SUGAR_MAX)
			throw DoughException.create("Sugar [%] must be between 0 and " + Helper.round(SUGAR_MAX * 100., 1) + "%");

		return this;
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
		this.salt += salt;

		if(salt < 0. || this.salt > SALT_MAX)
			throw DoughException.create("Salt [%] must be between 0 and " + Helper.round(SALT_MAX * 100., 1) + "%");

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

	private void validate() throws DoughException{
		if(water < HYDRATION_MIN || water > HYDRATION_MAX)
			throw DoughException.create("Hydration [%] must be between " + Helper.round(HYDRATION_MIN * 100., 1)
				+ "% and " + Helper.round(HYDRATION_MAX * 100., 1) + "%");
	}



	/**
	 * Find the initial yeast able to obtain a given volume expansion ratio after a series of consecutive stages at a given duration at
	 * temperature.
	 *
	 * @param leaveningStages   Data for stages.
	 * @param targetVolumeExpansionRatio   Maximum target volume expansion ratio to reach.
	 * @param targetVolumeExpansionRatioAtLeaveningStage   Leavening stage in which to reach the given volume expansion ratio (index
	 * 	between 0 and `leaveningStages.length`).
	 * @param stretchAndFoldStages   Stretch & Fold stages.
	 * @return	Yeast to use at first stage [%].
	 */
	public double backtrackStages(final LeaveningStage[] leaveningStages, final double targetVolumeExpansionRatio,
			final int targetVolumeExpansionRatioAtLeaveningStage, final StretchAndFoldStage[] stretchAndFoldStages) throws DoughException,
			YeastException{
		validate();
		if(targetVolumeExpansionRatioAtLeaveningStage < 0 || targetVolumeExpansionRatioAtLeaveningStage >= leaveningStages.length)
			throw DoughException.create("Target volume expansion ratio at leavening stage must be between 0 and "
				+ (leaveningStages.length - 1));
		if(stretchAndFoldStages != null){
			double totalLeaveningDuration = 0.;
			for(final LeaveningStage leaveningStage : leaveningStages)
				totalLeaveningDuration += leaveningStage.duration;
			double totalStretchAndFoldDuration = 0.;
			for(final StretchAndFoldStage stretchAndFoldStage : stretchAndFoldStages)
				totalStretchAndFoldDuration += stretchAndFoldStage.lapse;
			if(totalStretchAndFoldDuration > totalLeaveningDuration)
				LOGGER.warn("Duration of overall stretch & fold phases is longer than duration of leavening stages by "
					+ Helper.round(totalStretchAndFoldDuration - totalLeaveningDuration, 2) + " hrs");
		}

		try{
			final double ingredientsFactor = ingredientsFactor();
			final UnivariateFunction f = yeast -> {
				final double alpha = maximumRelativeVolumeExpansionRatio(yeast);
				double lambda = estimatedLag(yeast);
				LeaveningStage currentStage = leaveningStages[0];
				double volumeExpansionRatio = 0.;
				double duration = 0.;
				if(targetVolumeExpansionRatioAtLeaveningStage > 0){
					int stretchAndFoldIndex = 0;
					double stretchAndFoldDuration = 0.;
					for(int i = 1; i < leaveningStages.length; i ++){
						final LeaveningStage previousStage = leaveningStages[i - 1];
						duration += previousStage.duration;
						currentStage = leaveningStages[i];

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
						while(stretchAndFoldStages != null && stretchAndFoldIndex < stretchAndFoldStages.length){
							final StretchAndFoldStage stretchAndFoldStage = stretchAndFoldStages[stretchAndFoldIndex];
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
						if(i == targetVolumeExpansionRatioAtLeaveningStage)
							break;
					}
				}

				//NOTE: last `stage.volumeDecrease` is NOT taken into consideration!
				volumeExpansionRatio += yeastModel.volumeExpansionRatio(duration + currentStage.duration, lambda, alpha,
					currentStage.temperature, ingredientsFactor);
				return volumeExpansionRatio * (1. - currentStage.volumeDecrease) - targetVolumeExpansionRatio;
			};
			return solverYeast.solve(SOLVER_EVALUATIONS_MAX, f, 0., YEAST_MAX);
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
		final double saltLag = Math.log(1. + Math.exp(0.494 * (salt * 1000. / water - 84.)));
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
		final double kWaterChlorineDioxide = chlorineDioxideFactor();
		final double kWaterFixedResidue = fixedResidueFactor();
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
			return Math.min(Helper.evaluatePolynomial(SUGAR_COEFFICIENTS, sugar), 1.);
		if(sugar < SUGAR_MAX)
			return -0.3154 - 0.403 * Math.log(sugar);
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
	 *
	 * @return	Correction factor.
	 */
	double saltFactor(){
		//Lactobacillus pentosus (salt [g/l])
		//final double a = 1. - Math.log(Math.pow(1. + Math.exp(0.1696 * salt), 0.1896));
		//final double b = salt / (52.6363 + 0.0763 * salt);
		//return Math.exp(a * b);

		//final double a = 1. - Math.log(Math.pow(1. + Math.exp(0.0618 * salt), 0.0073));
		//final double b = salt / (1.0010 - 0.0006 * salt);
		//return Math.exp(a * b);

		//local maximum is at 0.01%
		return Math.max(Helper.evaluatePolynomial(SALT_COEFFICIENTS, salt - 0.000_1), 0.);
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
	double chlorineDioxideFactor(){
		return Math.max(1. - waterChlorineDioxide / WATER_CHLORINE_DIOXIDE_MAX, 0.);
	}

	/**
	 * TODO Se la durezza dell’acqua è troppo elevata la fermentazione subisce rallentamenti a causa della formazione di una struttura glutinica troppo rigida. In caso contrario, dove la durezza dell’acqua risulta essere troppo scarsa, l’impasto si presenta assai appiccicoso e poco manipolabile. In questo frangente sarà utile abbassare l’idratazione.
	 *
	 * @return	Correction factor.
	 */
	double fixedResidueFactor(){
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
