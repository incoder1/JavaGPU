/*
Copyright 2020 Viktor Gubin

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
#version 420 compatibility

#pragma optimize(on)

#ifdef GL_ES
precision mediump float;
#else
precision highp float;
#endif

invariant gl_Position;

uniform mat4 mvp;
uniform mat4 mv;
uniform mat4 nm;

layout(location = 0) in vec3 vertex_coord;
layout(location = 1) in vec3 vertex_normal;

out vec4 eye_norm;
out vec4 eye_pos;

void main(void) {
	vec4 vcoord = vec4( vertex_coord, 1.0 );
	eye_norm = normalize( nm * vec4(vertex_normal,0.0) );
	eye_pos = mv * vcoord;
	gl_Position = mvp * vcoord;
}
