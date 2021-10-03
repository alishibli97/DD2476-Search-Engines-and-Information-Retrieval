import numpy as np
import matplotlib.pyplot as plt

# exact = {k.strip().split(":")[0]:k.strip().split(":")[1] for k in open("pagerank/davis_top_30.txt").readlines()}

exact = np.array([float(k.strip().split(":")[1]) for k in open("pagerank/davis_top_30.txt").readlines()])

result1 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc1_100.txt").readlines()])
result2 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc1_1000.txt").readlines()])
result3 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc1_10000.txt").readlines()])

error = [0,0,0]
for i in range(len(exact)):
    error[0] += (result1[i] - exact[i])**2

for i in range(len(exact)):
    error[1] += (result2[i] - exact[i])**2

for i in range(len(exact)):
    error[2] += (result3[i] - exact[i])**2

x = [100,1000,10000]

plt.plot(x,error)
plt.savefig("results/mc1.jpg")
plt.show()

# result1 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc2_100.txt").readlines()])
# result2 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc2_1000.txt").readlines()])
# result3 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc2_10000.txt").readlines()])

# error = [0,0,0]
# for i in range(len(exact)):
#     error[0] += (result1[i] - exact[i])**2

# for i in range(len(exact)):
#     error[1] += (result2[i] - exact[i])**2

# for i in range(len(exact)):
#     error[2] += (result3[i] - exact[i])**2

# x = [100,1000,10000]

# plt.plot(x,error)
# plt.savefig("results/mc2.jpg")
# plt.show()

# result1 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc4_100.txt").readlines()])
# result2 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc4_1000.txt").readlines()])
# result3 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc4_10000.txt").readlines()])

# error = [0,0,0]
# for i in range(len(exact)):
#     error[0] += (result1[i] - exact[i])**2

# for i in range(len(exact)):
#     error[1] += (result2[i] - exact[i])**2

# for i in range(len(exact)):
#     error[2] += (result3[i] - exact[i])**2

# x = [100,1000,10000]

# plt.plot(x,error)
# plt.savefig("results/mc4.jpg")
# plt.show()

# result1 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc5_100.txt").readlines()])
# result2 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc5_1000.txt").readlines()])
# result3 = np.array([float(k.strip().split(" ")[2]) for k in open("results/mc5_10000.txt").readlines()])

# error = [0,0,0]
# for i in range(len(exact)):
#     error[0] += (result1[i] - exact[i])**2

# for i in range(len(exact)):
#     error[1] += (result2[i] - exact[i])**2

# for i in range(len(exact)):
#     error[2] += (result3[i] - exact[i])**2

# x = [100,1000,10000]

# plt.plot(x,error)
# plt.savefig("results/mc5.jpg")
# plt.show()