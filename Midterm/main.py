import cv2
import numpy as np
import os
import sys
import glob
import tensorflow as tf
import scipy.spatial as sp
from handshape_feature_extractor import HandShapeFeatureExtractor
import shutil
from numpy import genfromtxt

arr = os.listdir(os.path.join('traindata'))

def getVectorFromFrame(list_of_files): 
    model = HandShapeFeatureExtractor.get_instance()
    vectors = []
    video_names = []
    step = int(len(list_of_files) / 100)
    if step == 0:
        step = 1
    count = 0
    for each in list_of_files:
        img = cv2.imread(each)
        img = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        results = model.extract_feature(img)
        results = np.squeeze(results)
        vectors.append(results)
        video_names.append(os.path.basename(each))
        print(video_names)
        count = count + 1
        if count % step == 0:
            sys.stdout.write("-")
            sys.stdout.flush()

    return vectors


def frameExtractor(videopath, frames_path, count):
    if not os.path.exists(frames_path):
        os.mkdir(frames_path)
    cap = cv2.VideoCapture(videopath)
    video_length = int(cap.get(cv2.CAP_PROP_FRAME_COUNT)) - 1
    frame_no = int(video_length / 1.2) 
    cap.set(1, frame_no)
    ret, frame = cap.read()
    cv2.imwrite(frames_path + "/%#05d.png" % (count + 1), frame)


def getPenultimateLayer(frames_path, filename): 
    files = []
    path = os.path.join(frames_path, "*.png")
    frames = glob.glob(path)
    frames.sort()
    files = frames
    predicted_vector = getVectorFromFrame(files)
    print(predicted_vector)
    np.savetxt(filename, predicted_vector, delimiter=",")


def getGestureNum(test_vector, train_penulLayer):
    lst = []
    for each in train_penulLayer:
        lst.append(sp.distance.cosine(test_vector, each))
        gesture_num = lst.index(min(lst))
    
    return gesture_num

videofiles = []
video_loc_path = os.path.join('traindata')
video_path = os.path.join(video_loc_path, "*.mp4")
videos = glob.glob(video_path)
videofiles = videos

count = 0
for each in videofiles:
    frames_path = os.path.join(os.getcwd(), "frames2")
    frameExtractor(each, frames_path, count)
    count = count + 1
filename1 = 'trainingset_penLayer.csv'
frames_path2 = os.path.join(os.getcwd(), "frames2")
getPenultimateLayer(frames_path, filename1)

videofiles = []
video_loc_path = os.path.join('test')
video_path = os.path.join(video_loc_path, "*.mp4")
videos = glob.glob(video_path)
videofiles = videos

count = 0
for each in videofiles:
    frames_path = os.path.join(os.getcwd(), "frames")
    frameExtractor(each, frames_path, count)
    count = count + 1
filename2 = 'testset_penLayer.csv'
frames_path = os.path.join(os.getcwd(), "frames")
getPenultimateLayer(frames_path2, filename2)

shutil.rmtree(os.path.join(os.getcwd(), "frames"))

training_data = genfromtxt(filename1, delimiter=",")
test_data = genfromtxt(filename2, delimiter=",")
res = []
for each in test_data:
    res.append(getGestureNum(each, training_data)%17)

np.savetxt("Results.csv", res, delimiter=",", fmt='% d')
print(res)