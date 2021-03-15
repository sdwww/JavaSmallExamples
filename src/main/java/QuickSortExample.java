/**
 * 快速排序
 */
public class QuickSortExample {


    public static void main(String[] args) {
        QuickSortExample mergeSortExample = new QuickSortExample();
        int[] arr = {11, 44, 23, 67, 88, 65, 34, 48, 9, 12};
        mergeSortExample.quickSort(arr, 0, arr.length - 1);
        for (int num : arr) {
            System.out.println(num);
        }
    }

    private void quickSort(int[] arr, int start, int end) {
        if (start >= end) {
            return;
        }
        int pivot = arr[start];
        int left = start, right = end;
        while (left < right) {
            //从右往左扫描，找到第一个比基准值小的元素
            while (left < right && arr[right] > pivot) {
                right--;
            }
            //从左往右扫描，找到第一个比基准值大的元素
            while (left < right && arr[left] < pivot) {
                left++;
            }
            if (left < right) {
                int temp = arr[left];
                arr[left] = arr[right];
                arr[right] = temp;
            }
        }
        arr[left] = pivot;
        // 1.对前半部分进行排序
        quickSort(arr, start, left - 1);
        // 2.对后半部分进行排序
        quickSort(arr, left + 1, end);
    }
}
