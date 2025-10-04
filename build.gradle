class Solution {
    public int longestValidParentheses(String s) {

        Stack<Integer> stk = new Stack<>();
        boolean [] vis = new boolean[s.length()];

        for(int i = 0;i< s.length();i++){

            if(s.charAt(i) == '('){
                stk.push(i);
                continue;
            }
            else if (!stk.isEmpty()){ 
                vis[i] = true;
                vis[stk.pop()] = true;
            }
        }    
        int cur = 0;
        int max = 0;
        for(int i = 0;i< s.length();i++){
            if(vis[i] == true){
                cur+=1;
            }
            else{

                max = Math.max(cur,max);
                cur = 0;
            }
        }
        return max = Math.max(cur,max);
    }
}