import numpy as np


bored = 0.15

P = np.array([[0,1/3,1/3,1/3,0],[0,0,0,1,0],[0,0,0,0.5,0.5],[0,0,0,0,1],[0.2,0.2,0.2,0.2,0.2]])
J = np.array([[1/5]*5]*5)

G = P*(1-bored) + J*bored

x = np.array([1,0,0,0,0])

print(G)

# for i in range(18):
#     print(x)
#     x = np.dot(x,G)

# print(G)
# print()
# print(np.dot(x,G))