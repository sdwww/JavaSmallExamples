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

    public void quickSort(int[] arr, int startIndex, int endIndex) {
        if (startIndex >= endIndex) {
            return;
        }
        //找出基准
        int partition = partition(arr, startIndex, endIndex);
        //分成两边递归进行
        quickSort(arr, startIndex, partition - 1);
        quickSort(arr, partition + 1, endIndex);
    }

    //找基准
    private static int partition(int[] arr, int startIndex, int endIndex) {
        int pivot = arr[startIndex];
        int left = startIndex;
        int right = endIndex;
        while (left != right) {
            while (left < right && arr[right] > pivot) {
                right--;
            }
            while (left < right && arr[left] <= pivot) {
                left++;
            }
            //找到left比基准大，right比基准小，进行交换
            if (left < right) {
                swap(arr, left, right);
            }
        }
        //第一轮完成，让left和right重合的位置和基准交换，返回基准的位置
        swap(arr, startIndex, left);
        return left;
    }

    //两数交换
    public static void swap(int[] arr, int i, int j) {
        int temp = arr[i];
        arr[i] = arr[j];
        arr[j] = temp;
    }

}
