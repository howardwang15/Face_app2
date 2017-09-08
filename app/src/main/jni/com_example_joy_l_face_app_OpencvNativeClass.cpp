#include "com_example_joy_l_face_app_OpencvNativeClass.h"
#include "Helpers.h"
#include <stdlib.h>
#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <sstream>
#include <string.h>
#include <cmath>

#define PI 3.14159265358979323846

using namespace cv;
using namespace std;


Mat norm3;
Mat normalized;
bool b_normalized = false;
bool validLocation(int x, int y, int buffer);
vector<int> locations;
vector<int> face_locations;
CascadeClassifier faceCascade, eyesCascade;

const int BUFFER = 5;
int number = 0;
Mat selected(200, 200, CV_8UC3);




double toDegrees(double radians) {
	return radians * 180. / PI;
}

double toRadians(double degrees) {
	return degrees * PI / 180.;
}

void detect(Mat& frame, vector<Rect>& face_rects, vector<vector<Rect> >& eyes_rects) {
	Mat gray;
	cvtColor(frame, gray, CV_RGBA2GRAY);
	equalizeHist(gray, gray);
	faceCascade.detectMultiScale(gray, face_rects, 1.05, 3, 0, Size(40, 40), Size(500,500));
	if (face_rects.size() > 0) {
		for (int i = 0; i < face_rects.size(); i++) {
			Mat face_region = gray(face_rects[i]);
			vector<Rect> eyes;

			eyesCascade.detectMultiScale(face_region, eyes, 1.03, 2, CV_HAAR_DO_ROUGH_SEARCH, Size(5, 5), Size());
			eyes_rects.push_back(eyes);
		}
	}
}

struct Matrix {
	double translated_x;
	double translated_y;
	double original_center_x;
	double original_center_y;
	double theta;
	double scale;
};



Matrix* getMatrix(int t_eye_center_x1, int t_eye_center_y1, int t_eye_center_x2, int t_eye_center_y2, int o_eye_center_x1, int o_eye_center_y1, int o_eye_center_x2, int o_eye_center_y2) {
    LOGI("getting a matrix");
	Matrix* matrix = new Matrix;
	matrix->translated_x = (t_eye_center_x1 + t_eye_center_x2) / 2;
	matrix->translated_y = (t_eye_center_y1 + t_eye_center_y2) / 2; //center of the 2 eyes on target
	matrix->original_center_x = (o_eye_center_x1 + o_eye_center_x2) / 2;
	matrix->original_center_y = (o_eye_center_y1 + o_eye_center_y2) / 2;
	double original_distance_x = abs(o_eye_center_x1 - o_eye_center_x2), original_distance_y = abs(o_eye_center_y1 - o_eye_center_y2);
	matrix->theta = -toDegrees(atan(original_distance_y / original_distance_x)); //rotation displacement angle
	double original_distance = sqrt(pow(original_distance_x, 2) + pow(original_distance_y, 2)); //distance between 2 eyes on ori image
	double target_distance = abs(t_eye_center_x1 - t_eye_center_x2); //distance between the 2 eyes on the target image
	matrix->scale = original_distance / target_distance;
	return matrix;
}

Mat normalize(Mat& frame, Matrix* mat) {
    LOGI("normalizing");
	Mat normalized(200, 200, CV_8UC1);
	Mat gray;
	cvtColor(frame, gray, CV_BGR2GRAY);
	int count = 0;
	double cosAngle = cos(toRadians(mat->theta));
	double sinAngle = sin(toRadians(mat->theta));
	for (int i = 0; i < normalized.size().width; i++) {
		for (int j = 0; j < normalized.size().height; j++) {
			int translated_x = i- mat->translated_x;
			int translated_y = j- mat->translated_y;
			double mapped_x = (translated_x * cosAngle + translated_y * sinAngle) * mat->scale +mat->original_center_x;
			double mapped_y = (-1 * translated_x * sinAngle + translated_y * cosAngle) * mat->scale +mat->original_center_y;
			if (mapped_x <= 0 || mapped_y <= 0 || mapped_y >= gray.size().height-1 || mapped_x >= gray.size().width-1) {
				normalized.at<uchar>(j, i) = 0;
			}
			else {
				double weight_x = 1 - (mapped_x - (int)mapped_x);
				double weight_y = 1 - (mapped_y - (int)mapped_y);
				//cout << weight_x << ", " << weight_y << endl;
				uchar value = uchar((gray.at<uchar>((int)mapped_y, (int)mapped_x)) * weight_x * weight_y) + uchar((gray.at<uchar>((int)mapped_y + 1, (int)mapped_x)) * (1 - weight_x) * weight_y)
					+ uchar((gray.at<uchar>((int)mapped_y, (int)mapped_x + 1)) * weight_x * (1 - weight_y)) + uchar((gray.at<uchar>((int)mapped_y + 1, (int)mapped_x + 1)) * (1 - weight_x) * (1 - weight_y));
				//cout << (int)value << endl;
				normalized.at<uchar>(j, i) = value;
			}
		}
	}
	return normalized;
}
/*
struct Matrix {
	double translated_x;
	double translated_y;
	double original_center_x;
	double original_center_y;
	double theta;
	double scale;
};*/


JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_faceDetectionMain(JNIEnv *, jclass, jlong input){
    Mat & original = *(Mat*) input;
    bool using_good_phone = true;
    string file_name_face;
    string file_name_eyes;
    if(using_good_phone){
        file_name_face = "/sdcard/face_app/haarcascade_frontalface_alt2.xml";
        file_name_eyes = "/sdcard/face_app/haarcascade_eye_tree_eyeglasses.xml";
    } else {
        file_name_face = "/storage/external_sd/face_app/haarcascade_frontalface_alt2.xml";
        file_name_eyes = "/storage/external_sd/face_app/haarcascade_eye_tree_eyeglasses.xml";
    }
    if(!faceCascade.load(file_name_face)) {
       LOGE("Error: unable to load the face file", "");
    }
    if(!eyesCascade.load(file_name_eyes)){
      LOGE("Error: unable to load the eyes file", "");
    }
    vector<Rect> face_rects;
    vector<vector<Rect> > eyes_rects;
    int scale = 4;
    Mat dst = Mat(original.rows/scale,original.cols/scale,CV_8UC3);
    resize(original, dst, dst.size(), 0, 0, INTER_AREA);
    detect(dst, face_rects, eyes_rects);
    for(int i = 0; i < face_rects.size(); i++){
        int face_top_x = face_rects[i].tl().x*scale, face_top_y = face_rects[i].tl().y*scale, face_bottom_x = face_rects[i].br().x*scale, face_bottom_y = face_rects[i].br().y*scale;
        rectangle(original,Point(face_top_x, face_top_y), Point(face_bottom_x, face_bottom_y), Scalar(255, 255, 255),4);
        if(eyes_rects[i].size() == 2){
            for(int j = 0; j < 2; j++){
                int x1 = (eyes_rects[i][j].tl().x+face_rects[i].tl().x)*scale;
                int x2 = (eyes_rects[i][j].br().x+face_rects[i].tl().x)*scale;
                int y1 = (eyes_rects[i][j].tl().y+face_rects[i].tl().y)*scale;
                int y2 = (eyes_rects[i][j].br().y+face_rects[i].tl().y)*scale;
                //locations.push_back((x1 + x2)/2);
                //locations.push_back((y1 + y2)/2);
                rectangle(original, Point(x1, y1), Point(x2, y2), Scalar(255, 0, 0),4);
            }
        }
    }
}
JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_faceDetection(JNIEnv *, jclass, jlong input, jboolean touched, jint touched_x, jint touched_y) {
    LOGI("Entering face detection");
    Mat& original = *(Mat*) input;
    Matrix* matrix;
    bool using_good_phone = true;
    string file_name_face;
    string file_name_eyes;
    if(using_good_phone){
        file_name_face = "/sdcard/face_app/haarcascade_frontalface_alt2.xml";
        file_name_eyes = "/sdcard/face_app/haarcascade_eye.xml";
    } else {
        file_name_face = "/storage/external_sd/face_app/haarcascade_frontalface_alt2.xml";
        file_name_eyes = "/storage/external_sd/face_app/haarcascade_eye_tree_eyeglasses.xml";
    }
    if(!faceCascade.load(file_name_face)) {
       LOGE("Error: unable to load the face file", "");
    }
    if(!eyesCascade.load(file_name_eyes)){
      LOGE("Error: unable to load the eyes file", "");
    }
    vector<Rect> face_rects;
    vector<vector<Rect> > eyes_rects;
    //resize(original, original, Size(), 0.6, 0.65, INTER_AREA);
    //resize(original, original, Size(1920, 1080), 0, 0, INTER_LINEAR);

    Mat copy = original.clone();
    int scale = 4;
    Mat dst = Mat(original.rows / scale, original.cols / scale, CV_8UC3);
    LOGI("resizing original to destination");
    resize(original, dst, dst.size(), 0, 0, INTER_LINEAR);
    LOGI("resized original to destination");
    detect(dst, face_rects, eyes_rects);
    if (face_rects.size() > 0) {
        LOGI("face detected!");
    } else {
        LOGI("No face detected");
    }
    for(int i = 0; i < face_rects.size(); i++){
        int buffer = 3;
        int face_top_x = face_rects[i].tl().x*scale, face_top_y = face_rects[i].tl().y*scale, face_bottom_x = face_rects[i].br().x*scale, face_bottom_y = face_rects[i].br().y*scale;
        face_locations = {face_top_x, face_top_y, face_bottom_x, face_bottom_y};
        if(validLocation(touched_x, touched_y, BUFFER)) {
            LOGI("original width %d", original.cols);
            LOGI("original height %d", original.rows);

            number++;
            ostringstream temp;
            temp << "/sdcard/face_app/original_" << number << ".jpg";
            string fName = temp.str();
            imwrite(fName, original);
            LOGI("turning green");
            if(eyes_rects[i].size() == 2  && !b_normalized){
                for(int j = 0; j < 2; j++){
                    int x1 = (eyes_rects[i][j].tl().x+face_rects[i].tl().x)*scale;
                    int x2 = (eyes_rects[i][j].br().x+face_rects[i].tl().x)*scale;
                    int y1 = (eyes_rects[i][j].tl().y+face_rects[i].tl().y)*scale;
                    int y2 = (eyes_rects[i][j].br().y+face_rects[i].tl().y)*scale;
                    locations.push_back((x1 + x2)/2);
                    locations.push_back((y1 + y2)/2);
                    rectangle(original, Point(x1, y1), Point(x2, y2), Scalar(255, 0, 0),4);
                }
                matrix = getMatrix(75, 100, 125, 100, locations[0], locations[1], locations[2], locations[3]);

                ofstream locFile("/sdcard/face_app/locations.txt");
                locFile << "x center 1: " << locations[0] << endl << "y center 1: " << locations[1] << endl;
                locFile << "x center 2: " << locations[2] << endl << "y center 2: " << locations[3] << endl;
                locFile.close();

                ostringstream outputTemp;
                outputTemp << "/sdcard/face_app/matrix_" << number << ".txt";
                string output = outputTemp.str();
                ofstream oFile(output);
                oFile << "translated x: " << matrix->translated_x << endl << "translated y: " << matrix->translated_y << endl;
                oFile << "original center x: " << matrix->original_center_x << endl << "original center y: " << matrix->original_center_y << endl;
                oFile << "theta: " << matrix->theta << endl << "scale: " << matrix->scale;
                oFile << "width of original: " << original.cols << endl << "height of original: " << original.rows << endl;
                oFile.close();
                LOGI("copy width: %d", copy.cols);
                LOGI("copy width: %d", copy.cols);
                Mat normalized = normalize(copy, matrix);
                delete matrix;
                LOGI("normalized channels: %d", normalized.channels());
                LOGE("Wrote image");
                b_normalized = true;
                rectangle(original,Point(face_top_x, face_top_y), Point(face_bottom_x, face_bottom_y), Scalar(0, 255, 0),4);
                ostringstream oss, norm;
                oss << "/sdcard/face_app/detected_" << number << ".jpg";
                norm << "/sdcard/face_app/normed_" << number << ".jpg";
                string detected = oss.str();
                string iNormed = norm.str();
                imwrite(detected, original);
                imwrite(iNormed, normalized);
            }
        } else {
            if (eyes_rects[i].size() == 2) {
                cout << "located eyes\n";
                for (int j = 0; j < 2; j++) {
                    int x1 = (eyes_rects[i][j].tl().x + face_rects[i].tl().x)*scale;
                    int x2 = (eyes_rects[i][j].br().x + face_rects[i].tl().x)*scale;
                    int y1 = (eyes_rects[i][j].tl().y + face_rects[i].tl().y)*scale;
                    int y2 = (eyes_rects[i][j].br().y + face_rects[i].tl().y)*scale;
                    locations.push_back((x1 + x2)/2);
                    locations.push_back((y1 + y2)/2);
                    rectangle(original, Point(x1, y1), Point(x2, y2), Scalar(255, 0, 0), 1);

                }
            }
            rectangle(original,Point(face_top_x, face_top_y), Point(face_bottom_x, face_bottom_y), Scalar(255, 255, 255),4);
        }

        //LOGI("touched? %d", int(touched));
        }
    //}
}

JNIEXPORT jint JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_getNumEyes(JNIEnv *, jclass, jlong frame){
    /*
    Mat& input = *(Mat*) frame;
    selected = input.clone();
    vector<Rect> face;
    vector<vector<Rect> > eyes;
    //resize(selected, selected, Size(), 0.5, 0.5, INTER_AREA);
    LOGI("Selected width %d", selected.cols);
    LOGI("Selected height %d", selected.rows);
    detect(selected, face, eyes);
    int numEyes = 0;
    if(eyes.size() == 1) {
        LOGI("size of eyes is 1");

        numEyes = eyes[0].size();
    }
    return (jint)numEyes;*/
    return 0;

}
int getEyes(Mat& frame) {
    vector<Rect> face;
    vector<vector<Rect> > eyes;
    detect(frame, face, eyes);
    int numEyes = 0;
    if(eyes.size() == 0) numEyes = 0;
    else if(eyes.size() == 1) {
        numEyes = eyes[0].size();
    }
    return numEyes;
}

/*
void detect(Mat& frame, vector<Rect>& face_rects, vector<vector<Rect> >& eyes_rects) {
	Mat gray;
	cvtColor(frame, gray, CV_RGBA2GRAY);
	equalizeHist(gray, gray);
	faceCascade.detectMultiScale(gray, face_rects, 1.05, 3, 0, Size(40, 40), Size(500,500));
	if (face_rects.size() > 0) {
		for (int i = 0; i < face_rects.size(); i++) {
			Mat face_region = gray(face_rects[i]);
			vector<Rect> eyes;

			eyesCascade.detectMultiScale(face_region, eyes, 1.03, 2, CV_HAAR_DO_ROUGH_SEARCH, Size(5, 5), Size());
			eyes_rects.push_back(eyes);
		}
	}
}*/

JNIEXPORT jboolean JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_locationIsValid(JNIEnv *, jclass, jint x, jint y){
    return (jboolean)validLocation(x, y, BUFFER);
}

bool validLocation(int x, int y, int buffer){
    return (x > face_locations[0] - buffer && y > face_locations[1] - buffer && x < face_locations[2] + buffer && y < face_locations[2] + buffer) ? true : false;
}

JNIEXPORT void JNICALL Java_com_example_joy_1l_face_1app_OpencvNativeClass_normalize(JNIEnv *, jclass, jlong input, jlong normed){
    Mat& frame = *(Mat*) input;
    Mat& normalized = *(Mat*) normed;
    normalized = selected.clone();
    if(getEyes(normalized) != 2) {
        LOGI("invalid number of eyes");
        return;
    } else {
        LOGI("number of eyes is good");
    }
    Matrix* matrix = new Matrix();
    matrix = getMatrix(75, 100, 125, 100, locations[0], locations[1], locations[2], locations[3]);
    normalized = normalize(frame, matrix);
}

/*
Matrix* getMatrix(int t_eye_center_x1, int t_eye_center_y1, int t_eye_center_x2, int t_eye_center_y2, int o_eye_center_x1, int o_eye_center_y1, int o_eye_center_x2, int o_eye_center_y2) {
	Matrix* matrix = new Matrix;
	matrix->translated_x = (t_eye_center_x1 + t_eye_center_x2) / 2;
	matrix->translated_y = (t_eye_center_y1 + t_eye_center_y2) / 2; //center of the 2 eyes on target
	matrix->original_center_x = (o_eye_center_x1 + o_eye_center_x2) / 2;
	matrix->original_center_y = (o_eye_center_y1 + o_eye_center_y2) / 2;
	double original_distance_x = abs(o_eye_center_x1 - o_eye_center_x2), original_distance_y = abs(o_eye_center_y1 - o_eye_center_y2);
	matrix->theta = -(atan(original_distance_y / original_distance_x)); //rotation displacement angle
	double original_distance = sqrt(pow(original_distance_x, 2) + pow(original_distance_y, 2)); //distance between 2 eyes on ori image
	double target_distance = abs(t_eye_center_x1 - t_eye_center_x2); //distance between the 2 eyes on the target image
	matrix->scale = original_distance / target_distance;
	return matrix;
}

Mat normalize(Mat frame, Matrix* mat) {
	Mat normalized(200, 200, CV_8UC1);
    	Mat gray;
    	cvtColor(frame, gray, CV_BGR2GRAY);
    	int count = 0;
    	double cosAngle = cos(mat->theta);
    	double sinAngle = sin(mat->theta);
    	for (int i = 0; i < normalized.size().width; i++) {
    		for (int j = 0; j < normalized.size().height; j++) {
    			int translated_x = i- mat->translated_x;
    			int translated_y = j- mat->translated_y;
    			double mapped_x = (translated_x * cosAngle + translated_y * sinAngle) * mat->scale +mat->original_center_x;
    			double mapped_y = (-1 * translated_x * sinAngle + translated_y * cosAngle) * mat->scale +mat->original_center_y;
    			if (mapped_x <= 0 || mapped_y <= 0 || mapped_y >= gray.size().height-1 || mapped_x >= gray.size().width-1) {
    				normalized.at<uchar>(j, i) = 0;
    			}
    			else {
    				double weight_x = 1 - (mapped_x - (int)mapped_x);
    				double weight_y = 1 - (mapped_y - (int)mapped_y);
    				//cout << weight_x << ", " << weight_y << endl;
    				uchar value = uchar((gray.at<uchar>((int)mapped_y, (int)mapped_x)) * weight_x * weight_y) + uchar((gray.at<uchar>((int)mapped_y + 1, (int)mapped_x)) * (1 - weight_x) * weight_y)
    					+ uchar((gray.at<uchar>((int)mapped_y, (int)mapped_x + 1)) * weight_x * (1 - weight_y)) + uchar((gray.at<uchar>((int)mapped_y + 1, (int)mapped_x + 1)) * (1 - weight_x) * (1 - weight_y));
    				//cout << (int)value << endl;
    				normalized.at<uchar>(j, i) = value;
    			}
    		}
    	}
    	return normalized;
}*/