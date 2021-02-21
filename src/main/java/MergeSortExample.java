/**
 * 归并排序示例
 */
public class MergeSortExample {

    public static void main(String[] args) {
        MergeSortExample mergeSortExample = new MergeSortExample();
        int[] arr = {11, 44, 23, 67, 88, 65, 34, 48, 9, 12};
        mergeSortExample.mergeSort(arr, 0, arr.length - 1, new int[arr.length]);
        for (int num : arr) {
            System.out.println(num);
        }

    }

    private void mergeSort(int[] arr, int start, int end, int[] temp) {
        if (start >= end) {
            return;
        }
        int mid = (start + end) / 2;
        // 1.对前半部分进行排序
        mergeSort(arr, start, mid, temp);
        // 2.对后半部分进行排序
        mergeSort(arr, mid + 1, end, temp);
        // 3.对两部分进行合并
        merge(arr, start, mid, end, temp);
    }

    private void merge(int[] arr, int start, int mid, int end, int[] temp) {
        int i = 0;
        int j = start, k = mid + 1;
        while (j <= mid && k <= end) {
            if (arr[j] < arr[k]) {
                temp[i++] = arr[j++];
            } else {
                temp[i++] = arr[k++];
            }
        }
        while (j <= mid) {
            temp[i++] = arr[j++];
        }
        while (k <= end) {
            temp[i++] = arr[k++];
        }

        for (int t = 0; t < i; t++) {
            arr[t + start] = temp[t];
        }
    }
}
