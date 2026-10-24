// Personal subscription publisher. Node traffic never passes through this Worker.
const respond = (body,status=200,headers={}) => new Response(body,{status,headers:{'Cache-Control':'no-store','X-Content-Type-Options':'nosniff',...headers}});
const json = (value,status=200)=>respond(JSON.stringify(value),status,{'Content-Type':'application/json'});
function same(a,b){if(typeof a!=='string'||typeof b!=='string'||a.length!==b.length)return false;let x=0;for(let i=0;i<a.length;i++)x|=a.charCodeAt(i)^b.charCodeAt(i);return x===0;}
export default {
 async fetch(request,env){
  const u=new URL(request.url);
  if(u.pathname==='/health'&&request.method==='GET')return json({ok:true});
  if(!env.SUBSCRIPTIONS||!env.READ_TOKEN||!env.ADMIN_TOKEN)return respond('Service not configured',503);
  if(request.method==='GET'&&same(u.pathname,'/sub/'+env.READ_TOKEN)){
   const value=await env.SUBSCRIPTIONS.get('current');if(!value)return respond('No configuration published yet',404);
   const saved=JSON.parse(value);return respond(JSON.stringify(saved.config),200,{'Content-Type':'text/yaml; charset=utf-8','Content-Disposition':'inline; filename="vultr-subscription.yaml"','profile-update-interval':'1'});
  }
  if(u.pathname.startsWith('/api/')){
   if(!same(request.headers.get('Authorization'),'Bearer '+env.ADMIN_TOKEN))return respond('Unauthorized',401);
   if(request.method==='GET'&&u.pathname==='/api/status'){
    const value=await env.SUBSCRIPTIONS.get('current');if(!value)return json({published:false});const saved=JSON.parse(value);return json({published:true,revision:saved.revision,updated_at:saved.updated_at,nodes:saved.config.proxies.length});
   }
   if(request.method==='PUT'&&u.pathname==='/api/config'){
    if(Number(request.headers.get('Content-Length')||0)>1024*1024)return respond('Payload too large',413);
    const text=await request.text();if(new TextEncoder().encode(text).byteLength>1024*1024)return respond('Payload too large',413);
    let input;try{input=JSON.parse(text);}catch{return respond('Invalid JSON',400);}
    const c=input?.config;
    if(!c||typeof c!=='object'||!Array.isArray(c.proxies)||c.proxies.length===0||c.proxies.length>500||!Array.isArray(c['proxy-groups'])||!Array.isArray(c.rules))return respond('Expected a complete Clash configuration with 1..500 proxies',400);
    if(c.proxies.some(p=>!p||typeof p.name!=='string'||typeof p.type!=='string'||typeof p.server!=='string'||!Number.isInteger(p.port)||p.port<1||p.port>65535))return respond('Invalid proxy',400);
    const revision=crypto.randomUUID();const updated_at=new Date().toISOString();await env.SUBSCRIPTIONS.put('current',JSON.stringify({config:c,revision,updated_at}));return json({published:true,revision,updated_at,nodes:c.proxies.length});
   }
   return respond('Not found',404);
  }
  return respond('Not found',404);
 }
};
