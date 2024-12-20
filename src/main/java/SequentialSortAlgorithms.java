import java.util.Arrays;

public class SequentialSortAlgorithms {
    // Последовательная сортировка Шелла
    public static void shellSort(int[] array) {
        int n = array.length;
        for (int gap = n / 2; gap > 0; gap /= 2) {
            for (int i = gap; i < n; i++) {
                int temp = array[i];
                int j;
                for (j = i; j >= gap && array[j - gap] > temp; j -= gap) {
                    array[j] = array[j - gap];
                }
                array[j] = temp;
            }
        }
    }

    // Последовательная быстрая сортировка
    public static void quickSort(int[] array, int low, int high) {
        // Увеличиваем пороговое значение до 10000
        if (high - low < 10000) {
            Arrays.sort(array, low, high + 1);
            return;
        }
        
        // Выбираем медианный pivot для лучшей производительности
        int mid = low + (high - low) / 2;
        int pivot = medianOfThree(array, low, mid, high);
        swap(array, pivot, high);
        
        int pi = partition(array, low, high);
        if (pi - 1 > low) quickSort(array, low, pi - 1);
        if (pi + 1 < high) quickSort(array, pi + 1, high);
    }

    private static int partition(int[] array, int low, int high) {
        int pivot = array[high];
        int i = (low - 1);
        
        for (int j = low; j < high; j++) {
            if (array[j] <= pivot) {
                i++;
                int temp = array[i];
                array[i] = array[j];
                array[j] = temp;
            }
        }
        
        int temp = array[i + 1];
        array[i + 1] = array[high];
        array[high] = temp;
        
        return i + 1;
    }

    private static int medianOfThree(int[] array, int low, int mid, int high) {
        if (array[low] > array[mid]) swap(array, low, mid);
        if (array[mid] > array[high]) swap(array, mid, high);
        if (array[low] > array[mid]) swap(array, low, mid);
        return mid;
    }

    private static void swap(int[] array, int i, int j) {
        int temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }

    // Последовательная сортировка слиянием
    public static void mergeSort(int[] array, int left, int right) {
        if (left < right) {
            int middle = (left + right) / 2;
            mergeSort(array, left, middle);
            mergeSort(array, middle + 1, right);
            merge(array, left, middle, right);
        }
    }

    private static void merge(int[] array, int left, int middle, int right) {
        int[] temp = new int[right - left + 1];
        int i = left, j = middle + 1, k = 0;
        
        while (i <= middle && j <= right) {
            if (array[i] <= array[j]) {
                temp[k++] = array[i++];
            } else {
                temp[k++] = array[j++];
            }
        }
        
        while (i <= middle) {
            temp[k++] = array[i++];
        }
        
        while (j <= right) {
            temp[k++] = array[j++];
        }
        
        for (i = 0; i < temp.length; i++) {
            array[left + i] = temp[i];
        }
    }
} 