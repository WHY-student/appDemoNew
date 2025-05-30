增量操作说明
1. 将fine_name和fine2coarse和attribute转为inc的
2. 按照 船机车 的顺序排列新类名字，并添加到cls_name_inc后
3. 按照 机船车 的顺序将cls2fine_inc的第二行，第三行和第六行后面修改为正确的名称


                            String clsName = readFileFromAssets("templates/cls_name_temp.txt");
                            String cls2Fine = readFileFromAssets("templates/cls2fine_temp.txt");