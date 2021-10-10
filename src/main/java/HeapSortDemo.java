/**
 * 堆排序
 */
public class HeapSortDemo {

    public static void main(String[] args) {
        int[] nums = {6, 7, 8, 5, 1, 2, 3, 4};
        HeapSortDemo heapSortDemo = new HeapSortDemo();
        heapSortDemo.heapSort(nums);
        for (int num : nums) {
            System.out.println(num);
        }
    }


    public void heapSort(int[] nums) {
        int size = nums.length;
        // 第一步，初始化堆，最大堆，从小到大。目的是对元素排序
        // i从完全二叉树的第一个非叶子节点开始，也就是len/2-1开始(数组下标从0开始)
        for (int i = size / 2 - 1; i >= 0; i--) {
            adjust(nums, size, i);
        }
        // 第二步，交换堆顶（最大）元素和二叉堆的最后一个叶子节点元素。目的是交换元素
        // i从二叉堆的最后一个叶子节点元素开始，也就是len-1开始(数组下标从0开始)
        for (int i = size - 1; i >= 1; i--) {
            swap(nums, 0, i);
            // 交换完之后需要重新调整二叉堆，从头开始调整，此时Index=0
            adjust(nums, i, 0);
        }
    }

    /**
     * 初始化堆
     *
     * @param nums  待调整的二叉树(数组)
     * @param len   待调整的数组长度
     * @param index 待调整的节点下标，二叉树的第一个非叶子节点
     */
    private void adjust(int[] nums, int len, int index) {
        int left = 2 * index + 1;
        int right = 2 * index + 2;
        int maxIndex = index;
        //开始调整，左右孩子下标必须小于len，也就是确保index必须是非叶子节点
        if (left < len && nums[left] > nums[maxIndex]) {
            maxIndex = left;
        }
        if (right < len && nums[right] > nums[maxIndex]) {
            maxIndex = right;
        }
        //若发生了交换，则max肯定不等于index了。此时max节点值需要与index节点值交换，上面交换索引值，这里交换节点值
        if (maxIndex != index) {
            swap(nums, index, maxIndex);
            //交换完之后需要再次对max进行调整，因为此时max有可能不满足最大堆
            adjust(nums, len, maxIndex);
        }
    }

    private void swap(int[] nums, int source, int target) {
        int temp = nums[source];
        nums[source] = nums[target];
        nums[target] = temp;
    }
}
