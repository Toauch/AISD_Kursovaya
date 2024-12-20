import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelSortAlgorithms {
    private static final int THRESHOLD = 1000;

    // Параллельная сортировка Шелла
    public static void parallelShellSort(int[] array, int numThreads) {
        int n = array.length;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = 0; i < gap; i++) {
                final int start = i;
                final int gapFinal = gap;
                executor.submit(() -> {
                    for (int j = start; j < n; j += gapFinal) {
                        int temp = array[j];
                        int k;
                        for (k = j; k >= gapFinal && array[k - gapFinal] > temp; k -= gapFinal) {
                            array[k] = array[k - gapFinal];
                        }
                        array[k] = temp;
                    }
                });
            }
            try {
                executor.shutdown();
                executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            executor = Executors.newFixedThreadPool(numThreads);
        }
    }

    // Параллельная быстрая сортировка
    private static class ParallelQuickSort extends RecursiveAction {
        private final int[] array;
        private final int left;
        private final int right;
        private static final int THRESHOLD = 10000;

        ParallelQuickSort(int[] array, int left, int right) {
            this.array = array;
            this.left = left;
            this.right = right;
        }

        @Override
        protected void compute() {
            if (right - left < THRESHOLD) {
                Arrays.sort(array, left, right + 1);
                return;
            }

            if (left < right) {
                int mid = left + (right - left) / 2;
                int pivot = medianOfThree(array, left, mid, right);
                swap(array, pivot, right);
                
                int pi = partition(array, left, right);
                
                if (pi - left > THRESHOLD) {
                    invokeAll(new ParallelQuickSort(array, left, pi - 1));
                } else {
                    Arrays.sort(array, left, pi);
                }
                
                if (right - (pi + 1) > THRESHOLD) {
                    invokeAll(new ParallelQuickSort(array, pi + 1, right));
                } else {
                    Arrays.sort(array, pi + 1, right + 1);
                }
            }
        }

        private int partition(int[] array, int left, int right) {
            int pivot = array[right];
            int i = left - 1;

            for (int j = left; j < right; j++) {
                if (array[j] <= pivot) {
                    i++;
                    int temp = array[i];
                    array[i] = array[j];
                    array[j] = temp;
                }
            }

            int temp = array[i + 1];
            array[i + 1] = array[right];
            array[right] = temp;

            return i + 1;
        }

        private int medianOfThree(int[] array, int low, int mid, int high) {
            if (array[low] > array[mid]) {
                if (array[mid] > array[high]) {
                    return mid;
                } else if (array[low] > array[high]) {
                    return high;
                } else {
                    return low;
                }
            } else {
                if (array[low] > array[high]) {
                    return low;
                } else if (array[mid] > array[high]) {
                    return high;
                } else {
                    return mid;
                }
            }
        }

        private void swap(int[] array, int i, int j) {
            int temp = array[i];
            array[i] = array[j];
            array[j] = temp;
        }
    }

    // Параллельная сортировка слиянием
    static class ParallelMergeSort extends RecursiveAction {
        private final int[] array;
        private final int start;
        private final int end;

        ParallelMergeSort(int[] array, int start, int end) {
            this.array = array;
            this.start = start;
            this.end = end;
        }

        @Override
        protected void compute() {
            if (end - start <= THRESHOLD) {
                Arrays.sort(array, start, end);
                return;
            }
            int mid = start + (end - start) / 2;
            invokeAll(
                    new ParallelMergeSort(array, start, mid),
                    new ParallelMergeSort(array, mid, end)
            );
            merge(array, start, mid, end);
        }

        private void merge(int[] arr, int start, int mid, int end) {
            int[] temp = Arrays.copyOfRange(arr, start, end);
            int i = 0;
            int j = mid - start;
            int k = start;
            while (i < mid - start && j < end - start) {
                if (temp[i] <= temp[j]) {
                    arr[k++] = temp[i++];
                } else {
                    arr[k++] = temp[j++];
                }
            }
            while (i < mid - start) {
                arr[k++] = temp[i++];
            }
            while (j < end - start) {
                arr[k++] = temp[j++];
            }
        }
    }

    // Методы для запуска сортировок
    public static void quickSort(int[] array) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new ParallelQuickSort(array, 0, array.length - 1));
    }

    public static void mergeSort(int[] array) {
        ForkJoinPool pool = ForkJoinPool.commonPool();
        pool.invoke(new ParallelMergeSort(array, 0, array.length));
    }

    // Утилитные методы
    public static boolean isSorted(int[] array) {
        for (int i = 0; i < array.length - 1; i++) {
            if (array[i] > array[i + 1]) return false;
        }
        return true;
    }

    // Пример использования
    public static void main(String[] args) {
        int size = 1000000;
        int[] arr1 = new int[size];
        int[] arr2 = new int[size];
        int[] arr3 = new int[size];
        // Заполняем массивы случайными числами
        for (int i = 0; i < size; i++) {
            int num = (int) (Math.random() * size);
            arr1[i] = num;
            arr2[i] = num;
            arr3[i] = num;
        }
        // Тестируем все три алгоритма
        System.out.println("Тестирование параллельных алгоритмов сортировки:");

        long startTime = System.currentTimeMillis();
        parallelShellSort(arr1, Runtime.getRuntime().availableProcessors());
        System.out.println("Время Shell sort: " + (System.currentTimeMillis() - startTime) + "мс");
        System.out.println("Отсортирован корректно: " + isSorted(arr1));
        startTime = System.currentTimeMillis();
        quickSort(arr2);
        System.out.println("Время Quick sort: " + (System.currentTimeMillis() - startTime) + "мс");
        System.out.println("Отсортирован корректно: " + isSorted(arr2));
        startTime = System.currentTimeMillis();
        mergeSort(arr3);
        System.out.println("Время Merge sort: " + (System.currentTimeMillis() - startTime) + "мс");
        System.out.println("Отсортирован корректно: " + isSorted(arr3));
    }
}