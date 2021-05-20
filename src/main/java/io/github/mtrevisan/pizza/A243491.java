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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @see <a href="https://apps.dtic.mil/dtic/tr/fulltext/u2/a243491.pdf">Nelson. Computer modeling of the cooking process for pizza. 1991.</a>
 */
public class A243491{

	private static final Logger LOGGER = LoggerFactory.getLogger(A243491.class);

	//Stefan-Boltzmann constant [W / (m² · K⁴)]
	private static final double SIGMA = 5.670374419e-8;

	//[°C]
	public static final double ABSOLUTE_ZERO = 273.15;

	private static final int INDEX_PROTEIN = 0;
	private static final int INDEX_FAT = 1;
	private static final int INDEX_CARBOHYDRATE = 2;
	private static final int INDEX_FIBER = 3;
	private static final int INDEX_ASH = 4;
	private static final int INDEX_WATER = 5;

	/** Specific gas constant for dry air [J / (kg · K)]. */
	private static final double R_DRY_AIR = 287.05;
	/** Specific gas constant for water vapor [J / (kg · K)]. */
	private static final double R_WATER_VAPOR = 461.495;

	private static final double[] WATER_VAPOR_PRESSURE_COEFFICIENTS = {0.99999683, -9.0826951e-3, 7.8736169e-5, -6.1117958e-7, 4.3884187e-9, -2.9883885e-11, 2.1874425e-13, -1.7892321e-15, 1.1112018e-17, -3.0994571e-20};
	private static final double[] AIR_VISCOSITY_COEFFICIENTS = {170.258, 0.605434, -1.33200e-3};
	private static final double[] AIR_VISCOSITY_PRESSURE_COEFFICIENTS = {-2.44358e-3, 1.17237, 0.125541};
	private static final double[] AIR_CONDUCTIVITY_COEFFICIENTS = {-3.9333e-4, 1.0184e-4, -4.8574e-8, 1.5207e-11};
	private static final double[] AIR_SPECIFIC_HEAT_COEFFICIENTS = {0.251625, -9.2525e-5, 2.1334e-7, -1.0043e-10};
	private static final double[] WATER_VAPOR_SPECIFIC_HEAT_COEFFICIENTS = {0.452219, -1.29224e-4, 4.17008e-7, -2.00401e-10};
	private static final double[] AIR_PRANDTL_COEFFICIENTS = {1.393e9, 322000., -1200., 1.1};

	/** Ratio of molar mass of water to molar mass of air. */
	private static final double WATER_AIR_MOLAR_MASS_RATIO = 0.622;
	/** Specific heat of water [J / (kg · K)]. */
	private static final double WATER_SPECIFIC_HEAT = 4.184;


	public static void main(String[] a){
		A243491 test = new A243491();

		double initialDoughProtein = 0.12;
		/** dough fat component [%]. */
		double initialDoughFat = 0.06;
		/** dough carbohydrate component [%]. */
		double initialDoughCarbohydrate = 0.5;
		/** dough fiber component [%]. */
		double initialDoughFiber = 0.01;
		/** dough asj component [%]. */
		double initialDoughAsh = 0.02;
		/** dough moisture component [%]. */
		double initialDoughMoisture = 0.7;

		//tomato compositions:
		/** tomato moisture component [%]. */
		double initialTomatoMoisture = 0.8;

		//pre-baking conditions:
		/** weight of raw dough [g]. */
		double initialDoughWeight = 0.7;
		/** weight of tomato [g]. */
		double initialTomatoWeight = 0.2;
		/** weight of oil in pan [g]. */
		double initialOilWeight = 0.;
		/** thickness of shell [mm]. */
		double initialDoughThickness = 10.;
		/** temperature of pizza entering oven [°C]. */
		double initialPizzaTemperature = 23.;

		//post-baking conditions:
		/** weight of oil in pan [g]. */
		double finalOilWeight = 0.;
		/** weight of shell and tomato [g]. */
		double finalShellAndTomatoWeight = 0.;
		/** weight of water from toppings [g]. */
		double finalMoistureAbsorbedFromToppingsByShell = 0.;
		/** thickness of shell [mm]. */
		double finalShellThickness = 20.;
		/** thickness of crust [mm]. */
		double finalCrustThickness = 0.5;
		/** final tomato temperature [°C]. */
		double finalTomatoTemperature = 0.;

		//baking conditions:
		/** pan thickness [mm]. */
		double panThickness = 0.;
		/** pizza diameter [mm]. */
		double pizzaDiameter = 2. * Math.sqrt(220. * 250. / Math.PI);
		/** pan density [kg / m³]. */
		double panDensity = 0.;
		/** pan thermal conductivity [W / (m · K)]. */
		double panThermalConductivity = 0.;
		/** pan specific heat [J / (kg · K)]. */
		double panSpecificHeat = 0.;
		/** pan emissivity. */
		double panEmissivity = 0.;
		/** total cooking time [s]. */
		double totalCookingTime = 12. * 60.;
		/** oven temperature [°C]. */
		double ovenTemperature = 220.;
		/** air pressure [hPa]. */
		double airPressure = 1013.25;
		/** air relative humidity [%]. */
		double airRelativeHumidity = 0.5;
		/** oven air speed [m / s]. */
		double ovenAirSpeed = 2.;
		/** estimate for the effective diffusivity for the crust [cm² / s]. */
		double crustEffectiveDiffusivity = 0.;

		/** number of sections. */
		int shellNodes = 10;
		test.doThings(initialDoughProtein, initialDoughFat, initialDoughCarbohydrate, initialDoughFiber, initialDoughAsh, initialDoughMoisture, initialTomatoMoisture, initialDoughWeight, initialTomatoWeight, initialOilWeight, initialDoughThickness, initialPizzaTemperature, finalOilWeight, finalShellAndTomatoWeight, finalMoistureAbsorbedFromToppingsByShell, finalShellThickness, finalCrustThickness, finalTomatoTemperature, panThickness, pizzaDiameter, panDensity, panThermalConductivity, panSpecificHeat, panEmissivity, totalCookingTime, ovenTemperature, airPressure, airRelativeHumidity, ovenAirSpeed, crustEffectiveDiffusivity, shellNodes);
	}

	public void doThings(
			//dough compositions:
			/** dough protein component [%]. */
			double initialDoughProtein,
			/** dough fat component [%]. */
			double initialDoughFat,
			/** dough carbohydrate component [%]. */
			double initialDoughCarbohydrate,
			/** dough fiber component [%]. */
			double initialDoughFiber,
			/** dough asj component [%]. */
			double initialDoughAsh,
			/** dough moisture component [%]. */
			double initialDoughMoisture,

			//tomato compositions:
			/** tomato moisture component [%]. */
			double initialTomatoMoisture,

			//pre-baking conditions:
			/** weight of raw dough [g]. */
			double initialDoughWeight,
			/** weight of tomato [g]. */
			double initialTomatoWeight,
			/** weight of oil in pan [g]. */
			double initialOilWeight,
			/** thickness of shell [mm]. */
			double initialDoughThickness,
			/** temperature of pizza entering oven [°C]. */
			double initialPizzaTemperature,

			//post-baking conditions:
			/** weight of oil in pan [g]. */
			double finalOilWeight,
			/** weight of shell and tomato [g]. */
			double finalShellAndTomatoWeight,
			/** weight of water from toppings [g]. */
			double finalMoistureAbsorbedFromToppingsByShell,
			/** thickness of shell [mm]. */
			double finalShellThickness,
			/** thickness of crust [mm]. */
			double finalCrustThickness,
			/** final tomato temperature [°C]. */
			double finalTomatoTemperature,

			//baking conditions:
			/** pan thickness [mm]. */
			double panThickness,
			/** pizza diameter [mm]. */
			double pizzaDiameter,
			/** pan density [kg / m³]. */
			double panDensity,
			/** pan thermal conductivity [W / (m · K)]. */
			double panThermalConductivity,
			/** pan specific heat [J / (kg · K)]. */
			double panSpecificHeat,
			/** pan emissivity. */
			double panEmissivity,
			/** total cooking time [s]. */
			double totalCookingTime,
			/** oven temperature [°C]. */
			double ovenTemperature,
			/** air pressure [hPa]. */
			double airPressure,
			/** air relative humidity [%]. */
			double airRelativeHumidity,
			/** oven air speed [m / s]. */
			double ovenAirSpeed,
			/** estimate for the effective diffusivity for the crust [cm² / s]. */
			double crustEffectiveDiffusivity,

			/** number of sections. */
			int shellNodes
		){
		final double convectiveHeatTransferCookingZone = heatTransferCoefficient(ovenTemperature, airPressure, airRelativeHumidity,
			ovenAirSpeed, pizzaDiameter);

		shellNodes ++;
		final double initialDoughThicknessOverFinalDoughThickness = initialDoughThickness / finalShellThickness;
		final double doughSliceThickness = (finalShellThickness / 1000.) / (shellNodes - 1);
		final double[][] doughComponent = new double[6][shellNodes];
		final double[][] doughTemperature = new double[2][shellNodes];
		for(int index = 0; index < shellNodes; index ++){
			doughComponent[INDEX_PROTEIN][index] = initialDoughProtein;
			doughComponent[INDEX_FAT][index] = initialDoughFat;
			doughComponent[INDEX_CARBOHYDRATE][index] = initialDoughCarbohydrate;
			doughComponent[INDEX_FIBER][index] = initialDoughFiber;
			doughComponent[INDEX_ASH][index] = initialDoughAsh;
			doughComponent[INDEX_WATER][index] = initialDoughMoisture;
			doughTemperature[0][index] = initialPizzaTemperature;
		}
		double outsidePanTemperatureAtT;
		double insidePanTemperatureAtT;
		final double initialDoughVolume = (initialDoughThickness / 1000.) * Math.PI * Math.pow(pizzaDiameter / 2000., 2.);
		final double finalCrustVolume = (finalCrustThickness / 1000.) * Math.PI * Math.pow(pizzaDiameter / 2000., 2.);
		//[%]
		final double doughVoidSpace = 1. - initialDoughWeight / (1000. * initialDoughVolume * doughDensity(initialPizzaTemperature,
			doughComponent, 0));
		final double totalMoistureLossDuringCooking = initialDoughWeight - finalShellAndTomatoWeight
			+ finalMoistureAbsorbedFromToppingsByShell + initialTomatoWeight * initialTomatoMoisture - (initialOilWeight - finalOilWeight);
		final double tmp0 = finalCrustVolume * initialDoughWeight / (initialDoughVolume * (1. - doughVoidSpace));
		final double initialCrustMoisture = tmp0 * initialDoughMoisture;
		final double initialCrustProtein = tmp0 * initialDoughProtein;
		final double initialCrustFat = tmp0 * initialDoughFat;
		final double initialCrustCarbohydrates = tmp0 * initialDoughCarbohydrate;
		final double initialCrustFiber = tmp0 * initialDoughFiber;
		final double initialCrustAsh = tmp0 * initialDoughAsh;
		final double finalCrustMoisture = initialCrustMoisture - totalMoistureLossDuringCooking;
		final double finalCrustFat = initialCrustFat + (initialOilWeight - finalOilWeight);
		//[%]
		final double finalCrustMoisturePercent = finalCrustMoisture / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double finalCrustFatPercent = finalCrustFat / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double finalCrustProteinPercent = initialCrustProtein / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double finalCrustCarbohydratesPercent = initialCrustCarbohydrates / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double finalCrustFiberPercent = initialCrustFiber / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double finalCrustAshPercent = initialCrustAsh / (finalCrustMoisture + finalCrustFat + initialCrustProtein
			+ initialCrustCarbohydrates + initialCrustFiber + initialCrustAsh);
		//[%]
		final double[][] oil = new double[6][1];
		oil[1][0] = 1.;
		//steam in void of dough slice `index`
		final double[] steamInVoid = new double[shellNodes];
		//initial moisture content in a slice of dough
		final double initialMoistureContent = ((initialDoughWeight / 1000.) / initialDoughVolume) * (1. - doughVoidSpace)
			* (doughSliceThickness * initialDoughThicknessOverFinalDoughThickness) * initialDoughMoisture;
		//moisture in slice of dough at node `index`
		final double[] moistureContent = new double[shellNodes];
		for(int index = 0; index < shellNodes; index ++)
			moistureContent[index] = (index == 0 || index == shellNodes - 1? initialMoistureContent / 2.: initialMoistureContent);
		final double oilAbsorbedByCrustInDT = ((initialOilWeight - finalOilWeight) / 1000.) / (finalCrustThickness / 1000.)
			* doughSliceThickness;
		//moisture in slice of dough once it becomes crust
		final double moistureContentCrust = initialMoistureContent - ((totalMoistureLossDuringCooking / 1000.)
			/ (finalCrustThickness / 1000.) * doughSliceThickness);
		//mass of steam that fills the voids in the a slice of dough
		final double steamMass = 0.5228 * doughSliceThickness;
		Double doughConductivityCorrectionFactor = null;
		while(true){
			int doughSlicesExperiencingMoistureLoss = 1;
			double totalMoistureLossAtTimeT = 0.;
			//mass of oil in pan at time t
			double oilInPan = initialOilWeight / 1000.;
			//excess heat due to steam condensation passed to next dough slice
			double excessHeat = 0.;
			outsidePanTemperatureAtT = initialPizzaTemperature;
			insidePanTemperatureAtT = initialPizzaTemperature;
			Double doughSpecificHeat = null;
			for(int cookingTime = 0; cookingTime < totalCookingTime; cookingTime ++){
				final double tmp = (panThermalConductivity / (panThickness / 1000.)) * (outsidePanTemperatureAtT - insidePanTemperatureAtT);
				final double outsidePanTemperatureAtTPlusDT = outsidePanTemperatureAtT
					+ (convectiveHeatTransferCookingZone * (ovenTemperature - outsidePanTemperatureAtT)
					- tmp
					+ (panEmissivity * SIGMA * (Math.pow(ovenTemperature + ABSOLUTE_ZERO, 4.) - Math.pow(outsidePanTemperatureAtT + ABSOLUTE_ZERO, 4.))))
					* (2. / ((panThickness / 1000.) * panDensity * panSpecificHeat));
				final double oilTemperature = (insidePanTemperatureAtT + doughTemperature[0][0]) / 2.;
				final double oilDensity = doughDensity(oilTemperature, oil, 0);
				final double oilLayerThicknessAtT = (oilInPan / oilDensity) / (Math.PI * Math.pow(pizzaDiameter / 2000., 2.));
				if(oilLayerThicknessAtT <= 0.){
					crustEffectiveDiffusivity *= 2.;
					break;
				}

				//inside pan temperature at time t plus delta t
				final double insidePanTemperatureAtTPlusDT = insidePanTemperatureAtT
					+ (tmp - (doughConductivity(oilTemperature, oil, 1) / oilLayerThicknessAtT) * (insidePanTemperatureAtT - doughTemperature[0][0]))
					/ ((panThickness / 1000.) * panDensity
					* panSpecificHeat + (oilLayerThicknessAtT / 2.) * oilDensity * specificHeat(oilTemperature, oil, 0));
				Double doughDensity = null;
				if(doughTemperature[0][0] < 100.){
					final double oilThermalConductivity = doughConductivity(oilTemperature, oil, 0);
					final double oilSpecificHeat = specificHeat(oilTemperature, oil, 0);
					doughDensity = doughDensity(doughTemperature[0][0], doughComponent, 0) * (1. - doughVoidSpace);
					final double doughThermalConductivity = doughConductivity(doughTemperature[0][0], doughComponent, 0)
						* (1. - doughVoidSpace) * doughConductivityCorrectionFactor;
					doughSpecificHeat = specificHeat(doughTemperature[0][0], doughComponent, 0) * (1. - doughVoidSpace);
					doughTemperature[1][0] = ((oilThermalConductivity / oilLayerThicknessAtT)
						* (insidePanTemperatureAtT - doughTemperature[0][0]) - (doughThermalConductivity / (doughSliceThickness
						* initialDoughThicknessOverFinalDoughThickness)) * (doughTemperature[0][0] - doughTemperature[0][1]))
						/ (((doughSliceThickness * initialDoughThicknessOverFinalDoughThickness) / 2.) * doughDensity * doughSpecificHeat);
				}
				else
					excessHeat = 0.;

				if(doughTemperature[1][0] > 100.){
					final double heat = (doughTemperature[1][0] - 100.)
						* (doughSliceThickness * initialDoughThicknessOverFinalDoughThickness / 2.) * doughDensity * doughSpecificHeat;
					doughTemperature[1][0] = 100.;
					final double MSP = heat / 2444900.;
					if(MSP + steamInVoid[0] > steamMass){
						final double tmp2 = moistureContent[0] - (MSP + steamInVoid[0] - steamMass);
						if(tmp2 < moistureContentCrust){
							excessHeat = (moistureContentCrust - tmp2) * 2444900.;
							totalMoistureLossAtTimeT += moistureContent[0] - moistureContentCrust;
							moistureContent[0] = moistureContentCrust;
							oilInPan -= oilAbsorbedByCrustInDT / 2.;
							doughSlicesExperiencingMoistureLoss = 2;
							doughComponent[INDEX_PROTEIN][0] = finalCrustProteinPercent;
							doughComponent[INDEX_FAT][0] = finalCrustFatPercent;
							doughComponent[INDEX_CARBOHYDRATE][0] = finalCrustCarbohydratesPercent;
							doughComponent[INDEX_FIBER][0] = finalCrustFiberPercent;
							doughComponent[INDEX_ASH][0] = finalCrustAshPercent;
							doughComponent[INDEX_WATER][0] = finalCrustMoisturePercent;
						}
						else{
							final double PCMC = tmp2 / moistureContent[0];
							totalMoistureLossAtTimeT += MSP + steamInVoid[0] - steamMass;
							moistureContent[0] -= MSP + steamInVoid[0] - steamMass;
							rescale(PCMC, doughComponent, 0);
							steamInVoid[0] = steamMass;
							excessHeat = 0.;
						}
					}
					else{
						steamInVoid[0] += MSP;
						excessHeat = 0.;
					}
				}

				if(doughTemperature[0][0] == 100.){
					final double oilThermalConductivity = doughConductivity(oilTemperature, oil, 0);
					final double oilSpecificHeat = specificHeat(oilTemperature, oil, 0);
					doughDensity = doughDensity(doughTemperature[0][0], doughComponent, 0) * (1. - doughVoidSpace);
					final double doughThermalConductivity = doughConductivity(doughTemperature[0][0], doughComponent, 0)
						* (1. - doughVoidSpace) * doughConductivityCorrectionFactor;
					doughSpecificHeat = specificHeat(doughTemperature[0][0], doughComponent, 0) * (1. - doughVoidSpace);
					doughTemperature[1][0] = ((oilThermalConductivity / oilLayerThicknessAtT)
						* (insidePanTemperatureAtT - doughTemperature[0][0]) - (doughThermalConductivity
						/ (doughSliceThickness * initialDoughThicknessOverFinalDoughThickness)) * (doughTemperature[0][0] - doughTemperature[0][1]))
						/ ((doughSliceThickness * initialDoughThicknessOverFinalDoughThickness / 2.) * doughDensity * doughSpecificHeat);
					if(moistureContent[0] > moistureContentCrust){
						final double heat = (doughTemperature[1][0] - 100.)
							* (doughSliceThickness * initialDoughThicknessOverFinalDoughThickness / 2.) * doughDensity * doughSpecificHeat;
						doughTemperature[1][0] = 100.;
						final double MSP = heat / 2444900.;
						if(MSP + steamInVoid[0] > steamMass){
							final double tmp2 = moistureContent[0] - (MSP + steamInVoid[0] - steamMass);
							if(tmp2 < moistureContentCrust){
								excessHeat = (moistureContentCrust - tmp2) * 2444900.;
								totalMoistureLossAtTimeT += moistureContent[0] - moistureContentCrust;
								moistureContent[0] = moistureContentCrust;
								oilInPan -= oilAbsorbedByCrustInDT / 2.;
								doughSlicesExperiencingMoistureLoss = 2;
								doughComponent[INDEX_PROTEIN][0] = finalCrustProteinPercent;
								doughComponent[INDEX_FAT][0] = finalCrustFatPercent;
								doughComponent[INDEX_CARBOHYDRATE][0] = finalCrustCarbohydratesPercent;
								doughComponent[INDEX_FIBER][0] = finalCrustFiberPercent;
								doughComponent[INDEX_ASH][0] = finalCrustAshPercent;
								doughComponent[INDEX_WATER][0] = finalCrustMoisturePercent;
							}
							else{
								final double PCMC = tmp2 / moistureContent[0];
								totalMoistureLossAtTimeT += (MSP + steamInVoid[0] - steamMass);
								moistureContent[0] -= MSP + steamInVoid[0] - steamMass;
								rescale(PCMC, doughComponent, 0);
								steamInVoid[0] = steamMass;
								excessHeat = 0.;
							}
						}
						else{
							steamInVoid[0] = MSP;
							excessHeat = 0.;
						}
					}
					else
						excessHeat = 0.;
				}

				for(int m = 0; m < shellNodes - 1; m ++){
					doughDensity = doughDensity(doughTemperature[0][m], doughComponent, m) * (1. - doughVoidSpace);
					final double doughThermalConductivity = doughConductivity(doughTemperature[0][m], doughComponent, m) * (1. - doughVoidSpace)
						* doughConductivityCorrectionFactor;
					doughSpecificHeat = specificHeat(doughTemperature[0][m], doughComponent, m) * (1. - doughVoidSpace);
					doughTemperature[1][m] = doughTemperature[0][m] + ((doughThermalConductivity / (doughSliceThickness
						* initialDoughThicknessOverFinalDoughThickness)) * (doughTemperature[0][m] + doughTemperature[0][m + 1]
						- 2. * doughTemperature[0][m]) + excessHeat) / ((doughSliceThickness * initialDoughThicknessOverFinalDoughThickness)
						* doughDensity * doughSpecificHeat);
					if(doughSlicesExperiencingMoistureLoss != m){
						if(doughTemperature[1][m] > 100.){
							final double heat = (doughTemperature[1][m] - 100.) * doughSliceThickness * doughDensity * doughSpecificHeat;
							doughTemperature[1][m] = 100.;
							final double MSP = heat / 2444900.;
							if(MSP + steamInVoid[m] > steamMass){
								excessHeat = ((MSP + steamInVoid[m]) - steamMass) * 2444900.;
								steamInVoid[m] = steamMass;
							}
							else{
								excessHeat = 0.;
								steamInVoid[m] = steamInVoid[m] + MSP;
							}
						}
						else
							excessHeat = 0.;
					}
					else if(doughTemperature[1][m] > 100.){
						final double heat = (doughTemperature[1][m] - 100.) * doughSliceThickness * doughDensity * doughSpecificHeat;
						doughTemperature[1][m] = 100.;
						final double MSP = heat / 2444900.;
						if(steamInVoid[m] + MSP > steamMass){
							//moisture lost from slice
							double sliceMoistureLost = crustEffectiveDiffusivity * (steamInVoid[m] + MSP)
								/ Math.pow(doughSliceThickness * (m - 1.), 2.);
							if(moistureContent[m] - sliceMoistureLost < moistureContentCrust){
								sliceMoistureLost = moistureContent[m] - moistureContentCrust;
								totalMoistureLossAtTimeT += sliceMoistureLost;
								moistureContent[m] = moistureContentCrust;
								doughSlicesExperiencingMoistureLoss ++;
								doughComponent[INDEX_PROTEIN][m] = finalCrustProteinPercent;
								doughComponent[INDEX_FAT][m] = finalCrustFatPercent;
								doughComponent[INDEX_CARBOHYDRATE][m] = finalCrustCarbohydratesPercent;
								doughComponent[INDEX_FIBER][m] = finalCrustFiberPercent;
								doughComponent[INDEX_ASH][m] = finalCrustAshPercent;
								doughComponent[INDEX_WATER][m] = finalCrustMoisturePercent;
								doughTemperature[1][m] = 100.;
								excessHeat = (steamInVoid[m] + MSP - sliceMoistureLost - steamMass) * 2444900.;
								oilInPan -= oilAbsorbedByCrustInDT;
								steamInVoid[m] = steamMass;
							}
							else{
								totalMoistureLossAtTimeT += sliceMoistureLost;
								final double PCMC = (moistureContent[m] - (MSP + steamInVoid[m] - steamMass)) / moistureContent[m];
								moistureContent[m] -= sliceMoistureLost;
								excessHeat = (MSP + steamInVoid[m] - steamMass - sliceMoistureLost) * 2444900.;
								steamInVoid[m] = steamMass;
								rescale(PCMC, doughComponent, m);
							}
						}
						else
							steamInVoid[m] += MSP;
					}
					else
						excessHeat = 0.;
				}

				doughDensity = doughDensity(doughTemperature[0][shellNodes], doughComponent, shellNodes - 1) * (1. - doughVoidSpace);
				final double doughThermalConductivity = doughConductivity(doughTemperature[0][shellNodes], doughComponent, shellNodes - 1)
					* (1. - doughVoidSpace) * doughConductivityCorrectionFactor;
				doughSpecificHeat = specificHeat(doughTemperature[0][shellNodes], doughComponent, shellNodes - 1)
					* (1. - doughVoidSpace);
				doughTemperature[1][shellNodes] = doughTemperature[0][shellNodes] + (doughThermalConductivity / (doughSliceThickness
					* initialDoughThicknessOverFinalDoughThickness)) * (doughTemperature[0][shellNodes - 1] - doughTemperature[0][shellNodes])
					/ (doughSliceThickness * initialDoughThicknessOverFinalDoughThickness * doughDensity * doughSpecificHeat);
				excessHeat = 0.;
				System.arraycopy(doughTemperature[1], 0, doughTemperature[0], 0, shellNodes);
				outsidePanTemperatureAtT = outsidePanTemperatureAtTPlusDT;
				insidePanTemperatureAtT = insidePanTemperatureAtTPlusDT;
			}

			totalMoistureLossAtTimeT *= Math.PI * Math.pow(pizzaDiameter / 2000., 2.);
			if(Math.abs(totalMoistureLossAtTimeT - totalMoistureLossDuringCooking) / totalMoistureLossDuringCooking > 0.01){
				crustEffectiveDiffusivity *= totalMoistureLossDuringCooking / totalMoistureLossAtTimeT;
				LOGGER.debug("Calculated moisture content: {}%, Calculated final temperature: {} °C", totalMoistureLossAtTimeT,
					doughTemperature[0][shellNodes]);
			}
			else if(Math.abs(doughTemperature[0][shellNodes] - finalTomatoTemperature) / finalTomatoTemperature > 0.01)
				doughConductivityCorrectionFactor = finalTomatoTemperature / doughTemperature[0][shellNodes];
			else
				break;
		}
	}

	private void rescale(final double moisture, final double[][] params, final int index){
		final double scale = params[INDEX_PROTEIN][index] + params[INDEX_FAT][index] + params[INDEX_CARBOHYDRATE][index]
			+ params[INDEX_FIBER][index] + params[INDEX_ASH][index] + params[INDEX_WATER][index] * moisture;
		params[INDEX_PROTEIN][index] = params[INDEX_PROTEIN][index] / scale;
		params[INDEX_FAT][index] = params[INDEX_FAT][index] / scale;
		params[INDEX_CARBOHYDRATE][index] = params[INDEX_CARBOHYDRATE][index] / scale;
		params[INDEX_FIBER][index] = params[INDEX_FIBER][index] / scale;
		params[INDEX_ASH][index] = params[INDEX_ASH][index] / scale;
		params[INDEX_WATER][index] = params[INDEX_WATER][index] / scale;
	}

	/**
	 * Empirical equation that can be used for air speed from 2 to 20 m/s.
	 *
	 * @param airTemperature   temperature [°C].
	 * @param airPressure   air pressure [hPa].
	 * @param airRelativeHumidity   air relative humidity [%].
	 * @param airSpeed   air speed [m / s].
	 * @param pizzaDiameter   pizza diameter [mm].
	 * @return	convective heat transfer [W / (m² · K)].
	 */
	private double heatTransferCoefficient(final double airTemperature, final double airPressure, final double airRelativeHumidity,
			final double airSpeed, final double pizzaDiameter){
		//calculate air density [kg / m³]
		final double dryAirDensity = airPressure * 100. / (R_DRY_AIR * (airTemperature + ABSOLUTE_ZERO));
		final double waterVaporPressure = 6.1078 / Math.pow(Helper.evaluatePolynomial(WATER_VAPOR_PRESSURE_COEFFICIENTS, airTemperature), 8.);
		final double moistDensity = airRelativeHumidity * waterVaporPressure / (R_WATER_VAPOR * (airTemperature + ABSOLUTE_ZERO));
		final double airDensity = dryAirDensity + moistDensity;

		//calculate air dynamic viscosity [N · s / m²2]
		final double airViscosity0 = Helper.evaluatePolynomial(AIR_VISCOSITY_COEFFICIENTS, airTemperature);
		//convert [hPa] to [MPa]
		final double airViscosityP = Helper.evaluatePolynomial(AIR_VISCOSITY_PRESSURE_COEFFICIENTS, airPressure / 10_000.);
		final double airViscosity = 1.e-7 * (airViscosity0 + airViscosityP);

		//calculate air thermal conductivity [W / (m · K)]
		final double airConductivity = Helper.evaluatePolynomial(AIR_CONDUCTIVITY_COEFFICIENTS, airTemperature + ABSOLUTE_ZERO);

		//calculate air Prandtl number at 1000 hPa
		//specificHeat * airViscosity / airConductivity;
		final double prandtlNumber = 1.e9 / Helper.evaluatePolynomial(AIR_PRANDTL_COEFFICIENTS, airTemperature);

//		//https://backend.orbit.dtu.dk/ws/portalfiles/portal/117984374/PL11b.pdf
//		//[cal / (g · K)]
//		final double specificHeatAir = Helper.evaluatePolynomial(AIR_SPECIFIC_HEAT_COEFFICIENTS, airTemperature + ABSOLUTE_ZERO);
//		//[cal / (g · K)]
//		final double specificHeatWater = Helper.evaluatePolynomial(WATER_VAPOR_SPECIFIC_HEAT_COEFFICIENTS,
//			airTemperature + ABSOLUTE_ZERO);
//		//[J / (kg · K)]
//		final double specificHeat = WATER_SPECIFIC_HEAT
//			* (specificHeatAir + airRelativeHumidity * (WATER_AIR_MOLAR_MASS_RATIO * specificHeatWater - specificHeatAir))
//			/ (1. - (1. - WATER_AIR_MOLAR_MASS_RATIO) * airRelativeHumidity);
//		final double prandtlNumber2 = 1000. * specificHeat * airViscosity / airConductivity;

		final double reynoldsNumber = airDensity * airSpeed * pizzaDiameter / airViscosity;

		return (airConductivity / pizzaDiameter) * 0.228 * Math.pow(reynoldsNumber, 0.731) * Math.pow(prandtlNumber, 0.333);
	}

	private double doughConductivity(final double temperature, final double[][] params, final int index){
		final double protein = 0.17881 + (0.0011958 - 2.7178e-6 * temperature) * temperature;
		final double fat = 0.18071 + (-2.7604e-4 - 1.7749e-7 * temperature) * temperature;
		final double carbohydrate = 0.20141 + (0.0013874 - 4.3312e-6 * temperature) * temperature;
		final double fiber = 0.18331 + (0.0012497 - 3.1683e-6 * temperature) * temperature;
		final double ash = 0.32962 + (0.0014011 - 2.9069e-6 * temperature) * temperature;
		final double water = 0.57109 + (0.0017625 - 6.7036e-6 * temperature) * temperature;
		return protein * params[INDEX_PROTEIN][index]
			+ fat * params[INDEX_FAT][index]
			+ carbohydrate * params[INDEX_CARBOHYDRATE][index]
			+ fiber * params[INDEX_FIBER][index]
			+ ash * params[INDEX_ASH][index]
			+ water * params[INDEX_WATER][index];
	}

	private double doughDensity(final double temperature, final double[][] params, final int index){
		final double protein = 1329.9 - 0.5184 * temperature;
		final double fat = 925.59 - 0.41757 * temperature;
		final double carbohydrate = 1599.1 - 0.31046 * temperature;
		final double fiber = 1311.5 - 0.36589 * temperature;
		final double ash = 2423.8 - 0.28063 * temperature;
		final double water = 997.18 + (0.0031439 - 0.0037575 * temperature) * temperature;
		return protein * params[INDEX_PROTEIN][index]
			+ fat * params[INDEX_FAT][index]
			+ carbohydrate * params[INDEX_CARBOHYDRATE][index]
			+ fiber * params[INDEX_FIBER][index]
			+ ash * params[INDEX_ASH][index]
			+ water * params[INDEX_WATER][index];
	}

	/**
	 * http://www.mscampicchio.com/tecal_10.php
	 * Y. Choi and M.R. Okos (1986) Journal of Food Process and Applications 1(1): 93 – 101
	 * http://b.web.umkc.edu/beckerb/publications/journals/thermophysical.pdf
	 *
	 * @param temperature   temperature [°C].
	 * @return	Cp [J / (kg · K)]
	 */
	private double specificHeat(final double temperature, final double[][] params, final int index){
		final double protein = 2.0082 + (0.0012089 - 1.3129e-6 * temperature) * temperature;
		final double fat = 1.9842 + (0.0014733 - 4.8008e-6 * temperature) * temperature;
		final double carbohydrate = 1.5488 + (0.0019625 - 5.9399e-6 * temperature) * temperature;
		final double fiber = 1.8459 + (0.0018306 - 4.6509e-6 * temperature) * temperature;
		final double ash = 1.0926 + (0.0018896 - 3.6817e-6 * temperature) * temperature;
		final double water = 4.1289 + (-9.0864e-5 + 5.4761e-6 * temperature) * temperature;
		return 1000. * (protein * params[INDEX_PROTEIN][index]
			+ fat * params[INDEX_FAT][index]
			+ carbohydrate * params[INDEX_CARBOHYDRATE][index]
			+ fiber * params[INDEX_FIBER][index]
			+ ash * params[INDEX_ASH][index]
			+ water * params[INDEX_WATER][index]);
	}

}
