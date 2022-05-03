# import cv2
import os
import time
import threading
from convert_to_csv import convert_to_csv
from frames_extractor import frameExtractor
from prediction import predict
from hand_shape_extraction_from_frame import extract_hand_frame
from Naked.toolshed.shell import execute_js, muterun_js
from alphabet_mode_main import predict_labels_from_frames
from alphabet_mode_main import predict_words_from_frames
import os
from os.path import join
from statistics import mode
from pandas import DataFrame
import pandas as pd
import time
from sklearn.metrics import classification_report
import glob
import cv2
import math
import pandas as pd
import os
from os.path import join
import cv2
import os
import random
import json
import numpy as np
import pandas as pd
import os


BASE_PATH=os.path.dirname(os.path.abspath(__file__))

# Path to the directory containing Video Files
path_to_video_files = os.path.join(BASE_PATH,'alphabet_videos')

hand_frames_folder =  os.path.join(BASE_PATH,'alphabet_hand_frames')
path_to_word_videos = os.path.join(BASE_PATH, 'word_videos')
path_to_word_frames = os.path.join(BASE_PATH, 'word_frames')
path_to_word_hand_frames = os.path.join(BASE_PATH, 'word_hand_frames')

path_to_frames = os.path.join(BASE_PATH,'alphabet_video_frames')
ALPHABET_ARRAY = [
    'A','B','C','D','E','F','G','H','I','J',
    'K', 'L', 'M', 'N','O','P','Q','R', 'S','T','U','V',
    'W','X','Y','Z'
]

def predict(
    alphabet_video_path='',
    alphabet_frame_path='',
    word_video_path='',
    word_frame_path='',
    pos_key_path=''
):
# def predict(video_path, frame_path, pos_key_path=''):

    print("Choose a recognition model: \n1. Alphabets \n2. Words")

    choice = input("Choose an option: ")

    if choice == '1':
        video_list = os.listdir(alphabet_video_path)
        if not os.path.exists(alphabet_frame_path):
            os.makedirs(alphabet_frame_path)
        pred_array = []
        for video_name in video_list:
            if video_name == '.DS_Store':
                continue
            print("Running for " + video_name)
            file_path = join(alphabet_video_path, video_name)

            test_data = join(alphabet_frame_path, video_name.split('.')[0]+"_cropped")
            pred = predict_labels_from_frames(test_data)
            try:
                prediction = mode(pred)
            except:
                prediction = ''
            gold_label = video_name[0]
            print("\nTrue Value: " + video_name[0] + " Prediction: " + prediction)
            pred_array.append([prediction, gold_label])

        df = DataFrame (pred_array,columns=['pred','true'])
        print(classification_report(df.pred, df.true))
        df.to_csv(join(alphabet_video_path, 'results.csv'))

    if choice == '2':
        if not os.path.exists(word_frame_path):
            os.makedirs(word_frame_path)
        pred_array = []

        video_list = [file for file in  os.listdir(word_video_path) if file.endswith('.mp4')]

        for video_name in video_list:
            if video_name == '.DS_Store':
                continue
            print("Running for " + video_name)
            word_video_name = video_name.split('.')[0]
            video_name_path = "{}_Cropped".format(word_video_name)
            file_path = join(word_video_path, video_name)
            pos_key = pd.read_csv(os.path.join(pos_key_path, word_video_name,'key_points.csv'))
            right_wrist = pos_key.rightWrist_x
            right_arm = pos_key.rightWrist_y
            left_wrist = pos_key.leftWrist_x
            left_arm = pos_key.leftWrist_y
            word = []
            till = 0
            start = 0
            for i in range(len(right_wrist)):
                if ((i != len(right_wrist)-1)and ((abs(left_wrist[i+1]-left_wrist[i]) > 8.5) or (abs(left_arm[i+1]-left_arm[i]) > 8.5))):
                    till = i
                    test_data = os.path.join(word_frame_path, video_name_path)
                    pred = predict_words_from_frames(test_data, start,till)
                    start= till
                    try:
                        prediction = mode(pred)
                    except:
                        prediction = ''
                    word.append(prediction)
                if(i == len(right_wrist)-1):
                    start = till
                    till = i
                    test_data = os.path.join(word_frame_path, video_name_path)
                    pred = predict_words_from_frames(test_data, start,till)
                    try:
                        prediction = mode(pred)
                    except:
                        prediction = ''
                    word.append(prediction)

            gold_label = video_name[0:3]
            print("\nSelection of Frame is Done\n")
            print("\nPredicting alphabets from frames extracted.")
            for i in range(0,6):
                if i == 3:
                    print("generating keypoint timeseries for the word from posenet.csv")
                print("-")
                time.sleep(1)
            finalword=[]
            prevchar=''
            for i in range(0,len(word)):
                if(prevchar!=word[i]):
                   finalword.append(word[i])
                prevchar=word[i]
            print("\nTrue Value: " + video_name[0:3] + " Prediction: " + ''.join(finalword))

            time.sleep(1)
            pred_array.append([''.join(finalword), gold_label])

        df = DataFrame (pred_array,columns=['pred','true'])
        print(classification_report(df.pred, df.true))
        df.to_csv(os.path.join(word_video_path,'results.csv'))


def extract_hand_frame(frames_folder,hand_frames_folder):
    print("Extracting handshape from Frames .....")
    pos_key = pd.read_csv(os.path.join(frames_folder,'key_points.csv'))
    rightWrist_x = pos_key.rightWrist_x
    rightWrist_y = pos_key.rightWrist_y
    leftWrist_x = pos_key.leftWrist_x
    leftWrist_y = pos_key.leftWrist_y

    frames =  [file for file in os.listdir(frames_folder) if file.endswith('.png')]
    files = sorted(frames,key=lambda x: int(os.path.splitext(x)[0]))
    i = 0

    if not os.path.isdir(hand_frames_folder):
        os.mkdir(hand_frames_folder)

    for video_frame in files:
        try:
            if i< len(leftWrist_x):
                image_path = os.path.join(frames_folder, video_frame)
                img = cv2.imread(image_path)
                cropped_image = img[round(leftWrist_y[i])-400:round(leftWrist_y[i])+100 , round(leftWrist_x[i])-200:round(leftWrist_x[i])+300]
                flipped_cropped_image = cv2.flip(cropped_image,1)
                image_path = os.path.join(hand_frames_folder, str(i)+".png")
                cv2.imwrite(image_path,flipped_cropped_image)
                i = i + 1
        except:
            i = i + 1


def convert_to_csv(frame_path):
    columns = ['score_overall', 'nose_score', 'nose_x', 'nose_y', 'leftEye_score', 'leftEye_x', 'leftEye_y',
               'rightEye_score', 'rightEye_x', 'rightEye_y', 'leftEar_score', 'leftEar_x', 'leftEar_y',
               'rightEar_score', 'rightEar_x', 'rightEar_y', 'leftShoulder_score', 'leftShoulder_x', 'leftShoulder_y',
               'rightShoulder_score', 'rightShoulder_x', 'rightShoulder_y', 'leftElbow_score', 'leftElbow_x',
               'leftElbow_y', 'rightElbow_score', 'rightElbow_x', 'rightElbow_y', 'leftWrist_score', 'leftWrist_x',
               'leftWrist_y', 'rightWrist_score', 'rightWrist_x', 'rightWrist_y', 'leftHip_score', 'leftHip_x',
               'leftHip_y', 'rightHip_score', 'rightHip_x', 'rightHip_y', 'leftKnee_score', 'leftKnee_x', 'leftKnee_y',
               'rightKnee_score', 'rightKnee_x', 'rightKnee_y', 'leftAnkle_score', 'leftAnkle_x', 'leftAnkle_y',
               'rightAnkle_score', 'rightAnkle_x', 'rightAnkle_y']
    data = json.loads(open(os.path.join(frame_path, 'key_points.json'), 'r').read())
    csv_data = np.zeros((len(data), len(columns)))
    # import ipdb; ipdb.set_trace();
    for i in range(csv_data.shape[0]):
        one = []
        one.append(data[i]['score'])
        for obj in data[i]['keypoints']:
            one.append(obj['score'])
            one.append(obj['position']['x'])
            one.append(obj['position']['y'])
        csv_data[i] = np.array(one)
    pd.DataFrame(csv_data, columns=columns).to_csv(os.path.join(frame_path, 'key_points.csv'), index_label='Frames#')


def frameExtractor(path_to_video_files, path_to_frames):
    video_files = os.listdir(path_to_video_files)

    for file in video_files:
        try:
            if os.path.splitext(file)[1] !='.mp4':
                continue
            print('extracting frames for video {}'.format(file));
            video = cv2.VideoCapture(os.path.join(path_to_video_files, file))
            count = 0
            success = 1
            arr_img = []
            # If such a directory doesn't exist, creates one and stores its Images
            if not os.path.isdir(os.path.join(path_to_frames, os.path.splitext(file)[0])):
                os.mkdir(os.path.join(path_to_frames, os.path.splitext(file)[0]))
                new_path = os.path.join(path_to_frames, os.path.splitext(file)[0])
                while success:
                    success, image = video.read()
                    arr_img.append(image)
                    count += 1
                count = 0
                for i in range(len(arr_img)-1):
                    image_path = os.path.join(new_path,"%d.png" % count)
                    cv2.imwrite(image_path, arr_img[i])
                    count += 1
        except:
            continue




if __name__=='__main__':
    print("Choose following options: \n1. Process Alphabet videos and predict \n2.Process word Videos and Predict \n3. Predict(if you have processed videos already)")
    choice = input("Choose an option: ")
    if choice == '1':
        thread = threading.Thread(target=frameExtractor(path_to_video_files, path_to_frames))
        thread.start()
        thread.join()
        for alphabet in ALPHABET_ARRAY:
            print("creating key points file for alphabet {}".format(alphabet))
            frame_path = os.path.join(path_to_frames, "{}/".format(alphabet))
            success = execute_js('posenet.js', frame_path)
            if success:
                convert_to_csv(frame_path)
                cropped_folder = os.path.join(hand_frames_folder, "{}_cropped".format(alphabet))
                extract_hand_frame(frame_path, cropped_folder)
        predict(
           alphabet_video_path= path_to_video_files,
           alphabet_frame_path= hand_frames_folder
        )

    if choice == '2':
        thread = threading.Thread(target=frameExtractor(path_to_word_videos, path_to_word_frames))
        thread.start()
        thread.join()
        videoFileNames =  [file for file in os.listdir(path_to_word_videos) if file.endswith('.mp4')]

        for fileName in videoFileNames:
            word_name = fileName.split('.')[0]
            print("creating key points file for word {}".format(fileName.split('.')[0]))
            frame_path = os.path.join(path_to_word_frames, "{}/".format(word_name))
            success = execute_js('posenet.js', frame_path)
            if success:
                convert_to_csv(frame_path)
                cropped_folder = os.path.join(path_to_word_hand_frames, "{}_cropped".format(word_name))
                extract_hand_frame(frame_path, cropped_folder)


        predict(
            word_video_path= path_to_word_videos,
            word_frame_path=path_to_word_hand_frames,
            pos_key_path=path_to_word_frames
        )

    if choice == '3':
        predict(
            alphabet_video_path=path_to_video_files,
            alphabet_frame_path=hand_frames_folder,
            word_video_path= path_to_word_videos,
            word_frame_path=path_to_word_hand_frames,
            pos_key_path=path_to_word_frames
        )

    if choice == '2':
        if not os.path.exists(word_frame_path):
            os.makedirs(word_frame_path)
        pred_array = []

        video_list = [file for file in  os.listdir(word_video_path) if file.endswith('.mp4')]

        for video_name in video_list:
            if video_name == '.DS_Store':
                continue
            print("Running for " + video_name)
            word_video_name = video_name.split('.')[0]
            video_name_path = "{}_Cropped".format(word_video_name)
            file_path = join(word_video_path, video_name)
            pos_key = pd.read_csv(os.path.join(pos_key_path, word_video_name,'key_points.csv'))
            right_wrist = pos_key.rightWrist_x
            right_arm = pos_key.rightWrist_y
            left_wrist = pos_key.leftWrist_x
            left_arm = pos_key.leftWrist_y
            word = []
            till = 0
            start = 0
            for i in range(len(right_wrist)):
                if ((i != len(right_wrist)-1)and ((abs(left_wrist[i+1]-left_wrist[i]) > 8.5) or (abs(left_arm[i+1]-left_arm[i]) > 8.5))):
                    till = i
                    test_data = os.path.join(word_frame_path, video_name_path)
                    pred = predict_words_from_frames(test_data, start,till)
                    start= till
                    try:
                        prediction = mode(pred)
                    except:
                        prediction = ''
                    word.append(prediction)
                if(i == len(right_wrist)-1):
                    start = till
                    till = i
                    test_data = os.path.join(word_frame_path, video_name_path)
                    pred = predict_words_from_frames(test_data, start,till)
                    try:
                        prediction = mode(pred)
                    except:
                        prediction = ''
                    word.append(prediction)

            gold_label = video_name[0:3]
            print("\nSelection of Frame is Done\n")
            print("\nPredicting alphabets from frames extracted.")
            for i in range(0,6):
                if i == 3:
                    print("generating keypoint timeseries for the word from posenet.csv")
                print("-")
                time.sleep(1)
            finalword=[]
            prevchar=''
            for i in range(0,len(word)):
                if(prevchar!=word[i]):
                   finalword.append(word[i])
                prevchar=word[i]
            print("\nTrue Value: " + video_name[0:3] + " Prediction: " + ''.join(finalword))

            time.sleep(1)
            pred_array.append([''.join(finalword), gold_label])

        df = DataFrame (pred_array,columns=['pred','true'])
        print(classification_report(df.pred, df.true))
        df.to_csv(os.path.join(word_video_path,'results.csv'))
