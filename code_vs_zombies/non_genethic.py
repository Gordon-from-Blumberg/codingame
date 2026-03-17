import sys
import math
import random
import time

random.seed(37)

TURNS = 10
POPULATION = 20
MUTATION_CHANCE = 0.02

WIDTH = 16000
HEIGHT = 9000
ASH_SPEED = 1000
ZOMBIE_SPEED = 400

def debug(msg):
    print(msg, file=sys.stderr, flush=True)

def dist2(x1, y1, x2, y2):
    return (x1 - x2) ** 2 + (y1 - y2) ** 2

def dist(x1, y1, x2, y2):
    return math.sqrt(dist2(x1, y1, x2, y2))

def randAngle():
    return random.randint(0, 360)

def randDist():
    return random.randint(0, ASH_SPEED + 1)

class Gene:
    def __init__(self, angle, dist):
        self.angle = angle
        self.dist = dist

class DNA:
    def __init__(self):
        self.genes = []

class Unit:
    def __init__(self, id, x, y):
        self.id = id
        self.x = x
        self.y = y

class Human(Unit):
    pass

class Zombie(Unit):
    def __init__(self, id, x, y, targetX, targetY):
        Unit.__init__(self, id, x, y)
        self.targetX = targetX
        self.targetY = targetY

while True:
    x, y = [int(i) for i in input().split()]

    human_count = int(input())
    humans = []
    for i in range(human_count):
        human_id, human_x, human_y = [int(j) for j in input().split()]
        humans.append(Human(human_id, human_x, human_y))

    zombie_count = int(input())
    zombies = []
    zombieHumanDistMin = -1
    closestZombie = None
    for i in range(zombie_count):
        zombie_id, zombie_x, zombie_y, zombie_xnext, zombie_ynext = [int(j) for j in input().split()]
        zombies.append(Zombie(zombie_id, zombie_x, zombie_y, zombie_xnext, zombie_ynext))
        zombieHumanDist = -1
        for j in range(human_count):
            currentZombieHumanDist = dist2(humans[j].x, humans[j].y, zombies[i].x, zombies[i].y)
            if (zombieHumanDist == -1 or currentZombieHumanDist < zombieHumanDist) and dist(x, y, humans[j].x, humans[j].y) / ASH_SPEED <= math.sqrt(currentZombieHumanDist) / ZOMBIE_SPEED:
                zombieHumanDist = currentZombieHumanDist

        if zombieHumanDistMin == -1 or zombieHumanDistMin > zombieHumanDist:
            zombieHumanDistMin = zombieHumanDist
            closestZombie = zombies[i]

    print(f'{closestZombie.targetX} {closestZombie.targetY}')