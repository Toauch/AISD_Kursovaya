import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.apache.commons.math3.stat.regression.OLSMultipleLinearRegression;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Random;

public class SortingBenchmark {
    private static final int[] SIZES = {
        500000, 1000000, 1500000, 2000000, 2500000, 
        3000000, 3500000, 4000000, 4500000, 5000000,
        5500000, 6000000, 6500000, 7000000, 7500000,
        8000000, 8500000, 9000000, 9500000, 10000000
    };
    private static final int RUNS = 3;

    public static void main(String[] args) {
        benchmarkAll();
    }

    private static void benchmarkAll() {
        XYSeriesCollection[] datasets = new XYSeriesCollection[3];
        for (int i = 0; i < 3; i++) {
            datasets[i] = new XYSeriesCollection();
            datasets[i].addSeries(new XYSeries("Sequential"));
            datasets[i].addSeries(new XYSeries("Parallel"));
        }

        String[] cases = {"average"};
        for (String testCase : cases) {
            benchmarkCase(testCase, datasets);
        }

        String[] algorithms = {"Merge Sort", "Quick Sort", "Shell Sort"};
        for (int i = 0; i < 3; i++) {
            createChart(datasets[i], 
                algorithms[i] + " (Sequential vs Parallel)", 
                "Array Size", 
                "Time (ms)", 
                "charts/" + algorithms[i].toLowerCase().replace(" ", "_") + "_comparison.png");
        }
    }

    private static void benchmarkCase(String testCase, XYSeriesCollection[] datasets) {
        for (int size : SIZES) {
            double[][] seqTimes = new double[3][RUNS];
            double[][] parTimes = new double[3][RUNS];

            for (int run = 0; run < RUNS; run++) {
                int[] array = generateArray(size, testCase);
                
                final int[] array1 = array.clone();
                seqTimes[0][run] = measureTime(() -> SequentialSortAlgorithms.mergeSort(array1, 0, array1.length - 1));

                final int[] array2 = array.clone();
                seqTimes[1][run] = measureTime(() -> SequentialSortAlgorithms.quickSort(array2, 0, array2.length - 1));

                final int[] array3 = array.clone();
                seqTimes[2][run] = measureTime(() -> SequentialSortAlgorithms.shellSort(array3));

                final int[] array4 = array.clone();
                parTimes[0][run] = measureTime(() -> ParallelSortAlgorithms.mergeSort(array4));

                final int[] array5 = array.clone();
                parTimes[1][run] = measureTime(() -> ParallelSortAlgorithms.quickSort(array5));

                final int[] array6 = array.clone();
                parTimes[2][run] = measureTime(() -> ParallelSortAlgorithms.parallelShellSort(array6, 
                    Runtime.getRuntime().availableProcessors()));
            }

            for (int i = 0; i < 3; i++) {
                double seqAvg = Arrays.stream(seqTimes[i]).average().orElse(0.0);
                double parAvg = Arrays.stream(parTimes[i]).average().orElse(0.0);
                double speedup = seqAvg / parAvg;

                datasets[i].getSeries(0).add(size, seqAvg);
                datasets[i].getSeries(1).add(size, parAvg);

                System.out.printf("Size: %d, %s - Sequential: %.2f ms, Parallel: %.2f ms, Speedup: %.2fx%n",
                    size,
                    new String[]{"Merge Sort", "Quick Sort", "Shell Sort"}[i],
                    seqAvg,
                    parAvg,
                    speedup);
            }
            System.out.println("----------------------------------------");
        }
    }

    private static int[] generateArray(int size, String testCase) {
        int[] array = new int[size];
        Random random = new Random();

        switch (testCase) {
            case "best":
                for (int i = 0; i < size; i++) {
                    array[i] = i;
                }
                break;
            case "worst":
                for (int i = 0; i < size; i++) {
                    array[i] = size - i;
                }
                break;
            default: // average
                for (int i = 0; i < size; i++) {
                    array[i] = random.nextInt(size);
                }
        }
        return array;
    }

    private static double measureTime(Runnable task) {
        long startTime = System.nanoTime();
        task.run();
        return (System.nanoTime() - startTime) / 1_000_000.0; // Конвертация в миллисекунды
    }

    private static void createChart(XYSeriesCollection dataset, String title, String xLabel, String yLabel, String filename) {
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            xLabel,
            yLabel,
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );

        try {
            File file = new File(filename);
            file.getParentFile().mkdirs();
            ChartUtils.saveChartAsPNG(file, chart, 800, 600);
        } catch (IOException e) {
            System.err.println("Ошибка при сохранении графика: " + e.getMessage());
        }
    }

    private static double[] fitLogarithmicRegression(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            double logX = Math.log(x[i]);
            sumX += logX;
            sumY += y[i];
            sumXY += logX * y[i];
            sumX2 += logX * logX;
        }
        
        double b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double a = (sumY - b * sumX) / n;
        
        return new double[]{a, b, 0}; // c = 0 для упрощения
    }

    private static void plotResults(String filename, double[] sizes, double[][] times, String[] algorithmNames) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        // Добавляем реальные данные
        for (int i = 0; i < algorithmNames.length; i++) {
            XYSeries series = new XYSeries(algorithmNames[i] + " (actual)");
            for (int j = 0; j < sizes.length; j++) {
                series.add(sizes[j], times[i][j]);
            }
            dataset.addSeries(series);
            
            // Добавляем регрессионную кривую
            XYSeries regressionSeries = new XYSeries(algorithmNames[i] + " (regression)");
            double[] coeffs = fitLogarithmicRegression(sizes, times[i]);
            
            for (double x = sizes[0]; x <= sizes[sizes.length-1]; x += 1000) {
                double logX = Math.log(x);
                double y = coeffs[0] + coeffs[1] * logX + coeffs[2] * logX * logX;
                regressionSeries.add(x, y);
            }
            dataset.addSeries(regressionSeries);
        }

        // ... остальной код для создания графика
    }
} 