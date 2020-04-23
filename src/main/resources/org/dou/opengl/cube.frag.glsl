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

uniform mat4 light_pads;
uniform mat4 material_adse;
uniform	float material_shininess;

in vec4 eye_norm;
in vec4 eye_pos;

invariant out vec4 frag_color;

vec4 phong_shading(vec4 norm) {
	vec4 s;
	if(0.0 == light_pads[0].w)
		s = normalize( light_pads[0] );
	else
		s = normalize( light_pads[0] - eye_pos );
	vec4 v = normalize( -eye_pos );
	vec4 r = normalize( - reflect( s, norm ) );
	vec4 ambient = light_pads[1] * material_adse[0];
	float cos_theta = clamp( dot(s,norm), 0.0, 1.0 );
	vec4 diffuse = ( light_pads[2] * material_adse[1] ) * cos_theta;
	if( cos_theta > 0.0 ) {
		float shininess = pow( max( dot(r,v), 0.0 ), material_shininess );
		vec4 specular = (light_pads[3] * material_adse[2]) * shininess;
		return ambient + clamp(diffuse,0.0, 1.0) + clamp(specular, 0.0, 1.0);
	}
	return ambient + clamp(diffuse,0.0, 1.0);
}

void main(void) {
	vec4 diffuse_color = material_adse[1];
	if( gl_FrontFacing ) {
		frag_color = diffuse_color + phong_shading(eye_norm);	
	} else {
		frag_color =  diffuse_color + phong_shading(-eye_norm);
	}
} 