import os

f = open("pagerank/linksDavis.txt").readlines()

f = { int(i.split(';')[0]): list(filter(None,i.strip().split(';')[1].split(','))) for i in f }

inlinks = {}

for node,outs in f.items():
    for o in outs:
        if o not in inlinks:
            inlinks[o] = {node}
        inlinks[o].add(node)

print(inlinks['14714'])